package com.badlogic.gdx.backends.lwjgl.audio;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EFXUtil;

import java.nio.IntBuffer;

import static org.lwjgl.openal.EFX10.*;

public class ReverbOpenALAudio extends OpenALAudio {

    public static ReverbOpenALAudio instance = null;

    private IntBuffer auxSlots;
    private IntBuffer effects;
    private IntBuffer filters;

    public static final Reverb REVERB_SMALL = new Reverb(
            0.15F, 0.0F, 1.0F, 0.2F, 0.99F, 0.6F, 2.5F,
            0.001F, 1.26F, 0.011F, 0.944F, 0.16F
    );

    public static final Reverb REVERB_MEDIUM = new Reverb(
            0.55F, 0.0F, 1.0F, 0.3F, 0.99F, 0.7F, 0.2F,
            0.015F, 1.26F, 0.011F, 0.944F, 0.15F
    );

    public static final Reverb REVERB_BIG = new Reverb(
            1.68F, 0.1F, 1.0F, 0.5F, 0.99F, 0.7F, 0.0F,
            0.021F, 1.26F, 0.021F, 0.944F, 0.13F
    );

    public static final Reverb REVERB_LARGE = new Reverb(
            4.142F, 0.5F, 1.0F, 0.4F, 0.89F, 0.7F, 0.0F,
            0.025F, 1.26F, 0.021F, 0.944F, 0.11F
    );

    public static final Reverb[] REVERBS = {REVERB_SMALL, REVERB_MEDIUM, REVERB_BIG, REVERB_LARGE};

    private int directFilter;

    public boolean isReverbAvailable = false;

    public ReverbOpenALAudio(int simultaneousSources, int deviceBufferCount, int deviceBufferSize) {
        super(simultaneousSources, deviceBufferCount, deviceBufferSize);

        instance = this;
        isReverbAvailable = false;

        if(!EFXUtil.isEfxSupported())
            return;

        if(!EFXUtil.isEffectSupported(AL_EFFECT_EAXREVERB))
            return;

        if(!EFXUtil.isFilterSupported(AL_FILTER_LOWPASS))
            return;

        auxSlots = BufferUtils.newIntBuffer(4);
        for (int i = 0; i < 4; i++) //meh... for some reason alGenAuxiliaryEffectSlots(auxSlots) working incorrectly here
            auxSlots.put(i, alGenAuxiliaryEffectSlots());

        effects = BufferUtils.newIntBuffer(4);
        alGenEffects(effects);

        filters = BufferUtils.newIntBuffer(4);
        alGenFilters(filters);

        for (int i = 0; i < 4; i++) {
            alEffecti(effects.get(i), AL_EFFECT_TYPE, AL_EFFECT_EAXREVERB);
            alFilteri(filters.get(i), AL_FILTER_TYPE, AL_FILTER_LOWPASS);
            applyReverb(REVERBS[i], auxSlots.get(i), effects.get(i));
            checkAlError();
        }
        directFilter = alGenFilters();
        alFilteri(directFilter, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
        Gdx.app.log("Audio", "Successfully initialized sound reverbs and filters");
        isReverbAvailable = true;
    }

    public static ReverbOpenALAudio overrideAudio(Application app, LwjglApplicationConfiguration config){
        ReverbOpenALAudio audio = null;
        if(!LwjglApplicationConfiguration.disableAudio) {
            //Destroy original audio and replace with ours
            if (app.getAudio() instanceof OpenALAudio)
                ((OpenALAudio) app.getAudio()).dispose();
            try {
                audio = new ReverbOpenALAudio(config.audioDeviceSimultaneousSources, config.audioDeviceBufferCount,
                        config.audioDeviceBufferSize);
            } catch (Throwable t) {
                app.log("LwjglApplication", "Couldn't initialize audio, disabling audio", t);
                LwjglApplicationConfiguration.disableAudio = true;
            }
        }

        return audio;
    }

    @Override
    public ReverbSound newSound(FileHandle file) {
        return new ReverbSound(this, super.newSound(file));
    }

    public void applyReverb(Reverb reverb, int auxSlotId, int effectId){
        alEffectf(effectId, AL_EAXREVERB_DENSITY, reverb.density);
        alEffectf(effectId, AL_EAXREVERB_DIFFUSION, reverb.diffusion);
        alEffectf(effectId, AL_EAXREVERB_GAIN, reverb.gain);
        alEffectf(effectId, AL_EAXREVERB_GAINHF, reverb.gainHF);
        alEffectf(effectId, AL_EAXREVERB_DECAY_TIME, reverb.decayTime);
        alEffectf(effectId, AL_EAXREVERB_DECAY_HFRATIO, reverb.decayHFRatio);
        alEffectf(effectId, AL_EAXREVERB_REFLECTIONS_GAIN, reverb.reflectionsGain);
        alEffectf(effectId, AL_EAXREVERB_LATE_REVERB_GAIN, reverb.lateReverbGain);
        alEffectf(effectId, AL_EAXREVERB_LATE_REVERB_DELAY, reverb.lateReverbDelay);
        alEffectf(effectId, AL_EAXREVERB_AIR_ABSORPTION_GAINHF, reverb.airAbsirptionGainHF);
        alEffectf(effectId, AL_EAXREVERB_ROOM_ROLLOFF_FACTOR, reverb.roomRolloffFactor);

        alAuxiliaryEffectSloti(auxSlotId, AL_EFFECTSLOT_EFFECT, effectId);
    }

    public void applySourceSettings(int sourceId, float[] sendGain, float[] sendCutoff, float directCutoff, float directGain){
        if(!isReverbAvailable) return;
        for (int i = 0; i < 4; i++) {
            alFilterf(filters.get(i), AL_LOWPASS_GAIN, sendGain[i]);
            alFilterf(filters.get(i), AL_LOWPASS_GAINHF, sendCutoff[i]);
            AL11.alSource3i(sourceId, AL_AUXILIARY_SEND_FILTER, auxSlots.get(i), i, filters.get(i));
        }

        alFilterf(directFilter, AL_LOWPASS_GAIN, directGain);
        alFilterf(directFilter, AL_LOWPASS_GAINHF, directCutoff);
        AL10.alSourcei(sourceId, AL_DIRECT_FILTER, directFilter);

    }

    private void checkAlError(){
        int error = AL10.alGetError();
        if(error == AL10.AL_NO_ERROR){
            return;
        }

        if (error == AL10.AL_INVALID_NAME) throw new IllegalStateException("AL_INVALID_NAME");
        else if (error == AL10.AL_INVALID_ENUM) throw new IllegalStateException("AL_INVALID_ENUM");
        else if (error == AL10.AL_INVALID_VALUE) throw new IllegalStateException("AL_INVALID_VALUE");
        else if (error == AL10.AL_INVALID_OPERATION) throw new IllegalStateException("AL_INVALID_OPERATION");
        else if (error == AL10.AL_OUT_OF_MEMORY) throw new IllegalStateException("AL_OUT_OF_MEMORY");
        else throw new IllegalStateException("Unknown  AL exception: " + error);
    }

    /**
     * Settings for AL_EFFECT_EAXREVERB
     *
     * TODO: Maybe support for AL_EFFECT_REVERB later
     */
    public static class Reverb {
        /**
         * Controls reverb decay time. (similiar to echo)
         * Ranges from 0.1 (suitable for very small room) to 20.0 (large room)
         */
        public float decayTime;
        /**
         * Controls the coloration of the late reverb
         * Value range: (0; 1]
         */
        public float density;
        /**
         * Controls the echo density when the reverb is decaying
         * Value range: [0; 1]
         */
        public float diffusion;
        /**
         * Controls the reflected sound that the reverb adds to all sound sources attached to.
         * Value range [0; 1]
         * Default: 0.89
         */
        public float gain;
        /**
         * Controls the reflected sound at high frequences
         * Value range: [0; 1]
         * Default: 0.0
         */
        public float gainHF;
        /**
         * Adjusts the spectral quality of the [decayTime]
         * Value range: (0; 20.0]
         * Default: 0.83
         */
        public float decayHFRatio;
        /**
         * Overall amount of the initial reflections relative to [gain]
         * Value range: [0, 3.16] (from -100dB to +10dB)
         * Default: 0.05
         */
        public float reflectionsGain;
        /**
         * Delay between the arrival time of the direct path from the source to the first from the source.
         * Value range: [0; 0.3] (0 to 300 milliseconds)
         *
         */
        public float reflectionsDelay;
        /**
         * Controls the overall amount of LATE (not initial) reverb relative to the [gain]
         * Value range: [0; 10.0]
         * Default: 1.26
         */
        public float lateReverbGain;
        /**
         * Defines the starting time of the late reverb relative to the time of the initial reflection
         * Value range: [0.0; 0.1]
         * Default: 0.011
         */
        public float lateReverbDelay;
        /**
         * Controls the distance-dependent attenuation at high frequencies caused by the propagation minimum
         * Value range: [0.892; 1.0]
         * Default: 0.994
         */
        public float airAbsirptionGainHF;
        /**
         * Attenuates the reflected sound
         * Value range: [0.0; 10.0]
         * Default: 0.0
         */
        public float roomRolloffFactor;

        public Reverb(float decayTime, float density, float diffusion, float gain, float gainHF, float decayHFRatio,
                      float reflectionsGain, float reflectionsDelay, float lateReverbGain, float lateReverbDelay,
                      float airAbsirptionGainHF, float roomRolloffFactor) {
            this.decayTime = decayTime;
            this.density = density;
            this.diffusion = diffusion;
            this.gain = gain;
            this.gainHF = gainHF;
            this.decayHFRatio = decayHFRatio;
            this.reflectionsGain = reflectionsGain;
            this.reflectionsDelay = reflectionsDelay;
            this.lateReverbGain = lateReverbGain;
            this.lateReverbDelay = lateReverbDelay;
            this.airAbsirptionGainHF = airAbsirptionGainHF;
            this.roomRolloffFactor = roomRolloffFactor;
        }
    }
}
