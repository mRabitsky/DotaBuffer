package gui;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class HeroViewer extends JPanel {
	private BufferedImage background;

	public HeroViewer() {
		try {
			background=ImageIO.read(new File("res/background.png"));
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
	}
	
	@Override
	public void paint(Graphics g) {
		g.drawImage(background, 0, 0, null);
	}
}
