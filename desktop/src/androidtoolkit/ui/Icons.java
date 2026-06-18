package androidtoolkit.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;

public class Icons extends JPanel {

    public ImageIcon logo_tmo;
    public ImageIcon logo_att;
    public ImageIcon logo_product;
    public ImageIcon logo_senior;
    public ImageIcon frameIcon;
    public ImageIcon notInstalled;
    public ImageIcon logo_sprint;
    public ImageIcon logo_orange;
    public ImageIcon logo_dish;
    public ImageIcon logo_spc;
    public ImageIcon display_icon;

    public Icons() {

        //Set of scaled icons
        display_icon = scaleImageIcon("display.png", 25, 25);
        frameIcon = scaleImageIcon("Android.png", 25, 25);
        logo_dish = scaleImageIcon("dish.png", 45, 45);
        logo_tmo = scaleImageIcon("tmo.png", 45, 45);
        logo_att = scaleImageIcon("att.png", 45, 45);
        logo_product = scaleImageIcon("product.png", 45, 45);
        logo_senior = scaleImageIcon("Senior.png", 45, 45);
        logo_sprint = scaleImageIcon("sprint.png", 45, 45);
        logo_orange = scaleImageIcon("toyo.png", 45, 45);
        logo_spc = scaleImageIcon("spc.png", 45, 45);
        notInstalled = scaleImageIcon("Android.png", 45, 45);
    }

    public ImageIcon scaleImageIcon(String imageName, int width, int height) {
        ImageIcon icon = loadImageIcon(imageName);
        Image image = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private ImageIcon loadImageIcon(String imageName) {
        URL resource = getClass().getResource("/" + imageName);
        if (resource != null) {
            return new ImageIcon(resource);
        }

        File assetFile = new File("Assets", imageName);
        if (assetFile.isFile()) {
            return new ImageIcon(assetFile.getAbsolutePath());
        }

        throw new IllegalArgumentException("Could not load icon: " + imageName);
    }
}
