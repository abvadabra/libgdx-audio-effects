package demo;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl.audio.ReverbOpenALAudio;
import com.badlogic.gdx.backends.lwjgl.audio.ReverbSound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.Random;

public class DemoGame extends Game {

    private SpriteBatch batch;
    private BitmapFont font;

    private ReverbSound[] stepSounds = new ReverbSound[9];
    private int updateTick = 0;
    private Random rand = new Random();

    private int reverbPower = 100;
    private int cutoff = 100;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();

        for (int i = 0; i < stepSounds.length; i++)
            stepSounds[i] = ReverbOpenALAudio.instance
                    .newSound(Gdx.files.internal("sound/step/footstep_wood_walk_0" + (i + 1) + ".wav"));
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render() {
        super.render();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin();
        font.draw(batch,"Reverb power: " + (reverbPower) + "% (Use Up/Down to change)", 100, 100);
        font.draw(batch,"Low Pass Cutoff: " + (cutoff) + "% (Use Left/Right to change)", 100, 140);
        batch.end();

        if(Gdx.input.isKeyJustPressed(Input.Keys.UP))
            reverbPower = Math.min(100, reverbPower +  5);
        if(Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            reverbPower = Math.max(0, reverbPower - 5);
        if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))
            cutoff = Math.min(100, cutoff + 5);
        if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT))
            cutoff = Math.max(0, cutoff - 5);


        if(updateTick++ % 40 == 0){
            ReverbSound stepSound = stepSounds[rand.nextInt(stepSounds.length)];
            stepSound.setSettings(reverbPower / 100F, cutoff / 100F);
            stepSound.play(0.5F);
        }
    }
}
