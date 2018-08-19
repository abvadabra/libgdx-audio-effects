package demo;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.audio.ReverbOpenALAudio;

public class DemoApplication extends LwjglApplication {


    public DemoApplication(ApplicationListener listener, LwjglApplicationConfiguration config) {
        super(listener, config);

        Gdx.audio = audio = ReverbOpenALAudio.overrideAudio(this, config);
    }
}
