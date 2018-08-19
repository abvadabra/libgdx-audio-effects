package com.badlogic.gdx.backends.lwjgl.audio;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.LongMap;

import java.lang.reflect.Field;

public class ReverbSound extends OpenALSound {

    private static float[] EMPTY_ZERO_ARRAY = new float[4];
    private static float[] EMPTY_ONE_ARRAY = new float[]{1F, 1F, 1F, 1F};

    private static Field soundToSourceField;

    static {
        try {
            soundToSourceField = OpenALAudio.class.getDeclaredField("soundIdToSource");
            soundToSourceField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private final OpenALAudio audio;
    private final OpenALSound actualSound;

    private float[] sendGain = EMPTY_ZERO_ARRAY;
    private float[] sendCutoff = EMPTY_ONE_ARRAY;
    private float directCutoff = 1F;
    private float directGain = 1F;

    private long soundId = -1;

    public ReverbSound(OpenALAudio audio, OpenALSound actualSound) {
        super(audio);
        this.audio = audio;
        this.actualSound = actualSound;
    }

    public Integer getSourceId() {
        try {
            LongMap<Integer> soundIdToSource = (LongMap<Integer>) soundToSourceField.get(audio);
            return soundIdToSource.get(soundId);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void applySoundSettings(){
        Integer sourceId = getSourceId();
        if(sourceId != null && sourceId != -1)
        ReverbOpenALAudio.instance.applySourceSettings(sourceId,
                sendGain, sendCutoff, directCutoff, directGain);
    }

    public void setSettings(float[] sendGain, float[] sendCutoff, float directCutoff, float directGain){
        this.sendGain = sendGain;
        this.sendCutoff = sendCutoff;
        this.directCutoff = directCutoff;
        this.directGain = directGain;
    }

    public void setSettings(float reverbPower, float directCutoff){
        float[] sendGain = {
                MathUtils.clamp(reverbPower / 0.25F, 0F, 1F),
                MathUtils.clamp((reverbPower - 0.25F) / 0.25F, 0F, 1F),
                MathUtils.clamp((reverbPower - 0.5F) / 0.25F, 0F, 1F),
                MathUtils.clamp((reverbPower - 0.75F) / 0.25F, 0F, 1F)
        };
        setSettings(sendGain, new float[]{
                sendGain[0] * directCutoff,
                sendGain[1] * directCutoff,
                sendGain[2] * directCutoff,
                sendGain[2] * directCutoff

        }, directCutoff, 1F);
    }


    @Override
    public long play() {
        soundId = actualSound.play();
        if(soundId != -1)
            applySoundSettings();
        return soundId;
    }

    @Override
    public long play(float volume) {
        soundId = actualSound.play(volume);
        if(soundId != -1)
            applySoundSettings();
        return soundId;
    }

    @Override
    public long loop() {
        return actualSound.loop();
    }

    @Override
    public long loop(float volume) {
        return actualSound.loop(volume);
    }

    @Override
    public void stop() {
        actualSound.stop();
    }

    @Override
    public void dispose() {
        actualSound.dispose();
    }

    @Override
    public void stop(long soundId) {
        actualSound.stop(soundId);
    }

    @Override
    public void pause() {
        actualSound.pause();
    }

    @Override
    public void pause(long soundId) {
        actualSound.pause(soundId);
    }

    @Override
    public void resume() {
        actualSound.resume();
    }

    @Override
    public void resume(long soundId) {
        actualSound.resume(soundId);
    }

    @Override
    public void setPitch(long soundId, float pitch) {
        actualSound.setPitch(soundId, pitch);
    }

    @Override
    public void setVolume(long soundId, float volume) {
        actualSound.setVolume(soundId, volume);
    }

    @Override
    public void setLooping(long soundId, boolean looping) {
        actualSound.setLooping(soundId, looping);
    }

    @Override
    public void setPan(long soundId, float pan, float volume) {
        actualSound.setPan(soundId, pan, volume);
    }

    @Override
    public long play(float volume, float pitch, float pan) {
        soundId = actualSound.play(volume);
        actualSound.setPitch(soundId, pitch);
        actualSound.setPan(soundId, pan, volume);
        if(soundId != -1)
            applySoundSettings();
        return soundId;
    }

    @Override
    public long loop(float volume, float pitch, float pan) {
        return actualSound.loop(volume, pitch, pan);
    }

    public float duration() {
        return actualSound.duration();
    }

}
