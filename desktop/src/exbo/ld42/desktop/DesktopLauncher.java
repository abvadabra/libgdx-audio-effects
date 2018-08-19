package exbo.ld42.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import demo.DemoApplication;
import demo.DemoGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "DemoApp";
		config.width = 480;
		config.height = 220;

		new DemoApplication(new DemoGame(), config);
	}
}
