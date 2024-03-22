import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class Icons extends JPanel {

    public ImageIcon buttonIcon, logo_tmo, logo_att, logo_product, frameIcon, notInstalled, logo_sprint, logo_orange;

    public Icons() {
        //Set of scaled icons
        frameIcon = new ImageIcon(getToolkit().getImage(ClassLoader.getSystemResource("Android.png")));
        buttonIcon = scaleImageIcon("display.png", 25, 25);
        logo_tmo = scaleImageIcon("tmo.png", 45, 45);
        logo_att = scaleImageIcon("att.png", 45, 45);
        logo_product = scaleImageIcon("product.png", 45, 45);
        logo_sprint = scaleImageIcon("sprint.png", 45, 45);
        logo_orange = scaleImageIcon("toyo.png", 45, 45);
        notInstalled = scaleImageIcon("Android.png", 45, 45);
    }

    private ImageIcon scaleImageIcon(String imageName, int width, int height) {
        ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource(imageName)));
        Image image = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }
}