import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import Buttons.DevicesButton;
import Buttons.EnableFirebaseButtons;
import Buttons.FileButton;
import Buttons.InstallButton;
import Buttons.RebootButtons;
import Buttons.SaveSPLogsButtons;
import Buttons.TakeScreenshotButtons;
import Buttons.UninstallAllButton;
import Buttons.UninstallAppButtons;
import Buttons.WifiDebugButtons;
import Buttons.RadioButtons;

public class MyFrame extends JFrame implements PropertyChangeListener {

	Util command1 = new Util();
	ArrayList<String> output1 = command1.getConnectedDevices();
	private static int counter = 0;
	private static final Object lock = new Object();
	ArrayList<String> ips = new ArrayList<>();
	ImageIcon buttonIcon, logo_tmo, logo_att, logo_product, frameIcon, notInstalled;
	File file = null;

	SaveSPLogsButtons saveLogsButton1, saveLogsButton2, saveLogsButton3, saveLogsButton4;
	WifiDebugButtons wifiDebug1, wifiDebug2, wifiDebug3, wifiDebug4;
	EnableFirebaseButtons enableFirebase1, enableFirebase2, enableFirebase3, enableFirebase4;
	RebootButtons reboot1, reboot2, reboot3, reboot4;
	TakeScreenshotButtons takeScreenshotButton1, takeScreenshotButton2, takeScreenshotButton3, takeScreenshotButton4;
	UninstallAppButtons uninstallApp1, uninstallApp2, uninstallApp3, uninstallApp4;
	DevicesButton devicesButton;
	JButton fileButton;
	InstallButton installButton;
	UninstallAllButton uninstallAllButton;
	RadioButtons radio1, radio2, radio3, radio4;
	LogoIconLabels labelIcon1, labelIcon2, labelIcon3, labelIcon4;
	JTextPane staticPane;
	ProgressBar progressBar;
	FileTextField fileTextField;
	DeviceTextPanes device1TextPane, device2TextPane, device3TextPane, device4TextPane;
	boolean radio1State = false, radio2State = false, radio3State = false, radio4State = false;
	
	ArrayList<DeviceTextPanes> deviceTextPanes = new ArrayList<>();
	ArrayList<LogoIconLabels> logoIconLabels = new ArrayList<>();
	ArrayList<SaveSPLogsButtons> saveSPLogsButtons = new ArrayList<>();
	ArrayList<EnableFirebaseButtons> enableFirebaseButtons = new ArrayList<>();
	ArrayList<RebootButtons> rebootButtons = new ArrayList<>();
	ArrayList<TakeScreenshotButtons> takeScreenshotButtons = new ArrayList<>();
	ArrayList<UninstallAppButtons> uninstallAppButtons = new ArrayList<>();
	ArrayList<RadioButtons> radioButtons = new ArrayList<>();
	ArrayList<WifiDebugButtons> wifiDebugButtons = new ArrayList<>();

	public MyFrame() {

		File logs = new File("C:/AdbToolkit/logs");
		if (!logs.exists()){
			logs.mkdirs();
		}
		frameIcon = new ImageIcon(getToolkit().getImage(ClassLoader.getSystemResource("Android.png")));
		notInstalled = new ImageIcon(getToolkit().getImage(ClassLoader.getSystemResource("Android.png")));
		buttonIcon = new ImageIcon(getToolkit().getImage(ClassLoader.getSystemResource("display.png")));
		logo_tmo = new ImageIcon(getToolkit().getImage(ClassLoader.getSystemResource("tmo.png")));
		logo_att = new ImageIcon(getToolkit().getImage(ClassLoader.getSystemResource("att.png")));
		logo_product = new ImageIcon(getToolkit().getImage(ClassLoader.getSystemResource("product.png")));

		// Get list of wlan0 IP addresses
		for (String element : output1) {
			ips.add(command1.getWlanIp(element));
		}

		Image image1 = buttonIcon.getImage();
		Image newimg = image1.getScaledInstance(25, 25, java.awt.Image.SCALE_SMOOTH);
		buttonIcon = new ImageIcon(newimg);

		Image image2 = logo_tmo.getImage();
		newimg = image2.getScaledInstance(45, 45, java.awt.Image.SCALE_SMOOTH);
		logo_tmo = new ImageIcon(newimg);

		Image image3 = logo_att.getImage();
		newimg = image3.getScaledInstance(45, 45, java.awt.Image.SCALE_SMOOTH);
		logo_att = new ImageIcon(newimg);

		Image image4 = logo_product.getImage();
		newimg = image4.getScaledInstance(45, 45, java.awt.Image.SCALE_SMOOTH);
		logo_product = new ImageIcon(newimg);

		Image image5 = notInstalled.getImage();
		newimg = image5.getScaledInstance(45, 45, java.awt.Image.SCALE_SMOOTH);
		notInstalled = new ImageIcon(newimg);

		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu editMenu = new JMenu("Edit");
		JMenu helpMenu = new JMenu("Help");
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(helpMenu);
		this.setJMenuBar(menuBar);

		device1TextPane = new DeviceTextPanes(200, 50, 200, 110);
		deviceTextPanes.add(device1TextPane);
		device2TextPane = new DeviceTextPanes(410, 50, 200, 110);
		deviceTextPanes.add(device2TextPane);
		device3TextPane = new DeviceTextPanes(620, 50, 200, 110);
		deviceTextPanes.add(device3TextPane);
		device4TextPane = new DeviceTextPanes(830, 50, 200, 110);
		deviceTextPanes.add(device4TextPane);
		for (DeviceTextPanes deviceTextPane : deviceTextPanes) {
			this.add(deviceTextPane);
		}

		labelIcon1 = new LogoIconLabels(notInstalled, 275, 0, 122, 50);
		logoIconLabels.add(labelIcon1);
		labelIcon2 = new LogoIconLabels(notInstalled, 485, 0, 122, 50);
		logoIconLabels.add(labelIcon2);
		labelIcon3 = new LogoIconLabels(notInstalled, 695, 0, 122, 50);
		logoIconLabels.add(labelIcon3);
		labelIcon4 = new LogoIconLabels(notInstalled, 905, 0, 122, 50);
		logoIconLabels.add(labelIcon4);
		for (LogoIconLabels IconLabel : logoIconLabels) {
			this.add(IconLabel);
		}

		saveLogsButton1 = new SaveSPLogsButtons(200, 160, 95, 30);
		saveLogsButton1.addActionListener(new SaveLogsButton1Listener());
		saveSPLogsButtons.add(saveLogsButton1);
		saveLogsButton2 = new SaveSPLogsButtons(410, 160, 95, 30);
		saveLogsButton2.addActionListener(new SaveLogsButton2Listener());
		saveSPLogsButtons.add(saveLogsButton2);
		saveLogsButton3 = new SaveSPLogsButtons(620, 160, 95, 30);
		saveLogsButton3.addActionListener(new SaveLogsButton3Listener());
		saveSPLogsButtons.add(saveLogsButton3);
		saveLogsButton4 = new SaveSPLogsButtons(830, 160, 95, 30);
		saveLogsButton4.addActionListener(new SaveLogsButton4Listener());
		saveSPLogsButtons.add(saveLogsButton4);
		for (SaveSPLogsButtons saveLogsButton : saveSPLogsButtons) {
			this.add(saveLogsButton);
		}

		enableFirebase1 = new EnableFirebaseButtons(200, 220, 95, 30);
		enableFirebase1.addActionListener(new EnableFirebase1Listener());
		enableFirebaseButtons.add(enableFirebase1);
		enableFirebase2 = new EnableFirebaseButtons(410, 220, 95, 30);
		enableFirebase2.addActionListener(new EnableFirebase2Listener());
		enableFirebaseButtons.add(enableFirebase2);
		enableFirebase3 = new EnableFirebaseButtons(620, 220, 95, 30);
		enableFirebase3.addActionListener(new EnableFirebase3Listener());
		enableFirebaseButtons.add(enableFirebase3);
		enableFirebase4 = new EnableFirebaseButtons(830, 220, 95, 30);
		enableFirebase4.addActionListener(new EnableFirebase4Listener());
		enableFirebaseButtons.add(enableFirebase4);
		for (EnableFirebaseButtons firebaseButton : enableFirebaseButtons) {
			this.add(firebaseButton);
		}

		reboot1 = new RebootButtons(200, 190, 95, 30);
		reboot1.addActionListener(new Reboot1Listener());
		rebootButtons.add(reboot1);
		reboot2 = new RebootButtons(410, 190, 95, 30);
		reboot2.addActionListener(new Reboot2Listener());
		rebootButtons.add(reboot2);
		reboot3 = new RebootButtons(620, 190, 95, 30);
		reboot3.addActionListener(new Reboot3Listener());
		rebootButtons.add(reboot3);
		reboot4 = new RebootButtons(830, 190, 95, 30);
		reboot4.addActionListener(new Reboot4Listener());
		rebootButtons.add(reboot4);
		for (RebootButtons rebootButton : rebootButtons) {
			this.add(rebootButton);
		}

		takeScreenshotButton1 = new TakeScreenshotButtons(310, 190, 90, 30);
		takeScreenshotButton1.addActionListener(new TakeScreenshotButton1Listener());
		takeScreenshotButtons.add(takeScreenshotButton1);
		takeScreenshotButton2 = new TakeScreenshotButtons(520, 190, 90, 30);
		takeScreenshotButton2.addActionListener(new TakeScreenshotButton2Listener());
		takeScreenshotButtons.add(takeScreenshotButton2);
		takeScreenshotButton3 = new TakeScreenshotButtons(730, 190, 90, 30);
		takeScreenshotButton3.addActionListener(new TakeScreenshotButton3Listener());
		takeScreenshotButtons.add(takeScreenshotButton3);
		takeScreenshotButton4 = new TakeScreenshotButtons(940, 190, 90, 30);
		takeScreenshotButton4.addActionListener(new TakeScreenshotButton4Listener());
		takeScreenshotButtons.add(takeScreenshotButton4);
		for (TakeScreenshotButtons screenshotButton : takeScreenshotButtons) {
			this.add(screenshotButton);
		}

		uninstallApp1 = new UninstallAppButtons(310, 220, 90, 30);
		uninstallApp1.addActionListener(new UninstallApp1Listener());
		uninstallAppButtons.add(uninstallApp1);
		uninstallApp2 = new UninstallAppButtons(520, 220, 90, 30);
		uninstallApp2.addActionListener(new UninstallApp2Listener());
		uninstallAppButtons.add(uninstallApp2);
		uninstallApp3 = new UninstallAppButtons(730, 220, 90, 30);
		uninstallApp3.addActionListener(new UninstallApp3Listener());
		uninstallAppButtons.add(uninstallApp3);
		uninstallApp4 = new UninstallAppButtons(940, 220, 90, 30);
		uninstallApp4.addActionListener(new UninstallApp4Listener());
		uninstallAppButtons.add(uninstallApp4);
		for (UninstallAppButtons uninstallButton : uninstallAppButtons) {
			this.add(uninstallButton);
		}

		radio1 = new RadioButtons("Device 1", 200, 20, 75, 15);
		radioButtons.add(radio1);
		radio2 = new RadioButtons("Device 2", 410, 20, 75, 15);
		radioButtons.add(radio2);
		radio3 = new RadioButtons("Device 3", 620, 20, 75, 15);
		radioButtons.add(radio3);
		radio4 = new RadioButtons("Device 4", 830, 20, 75, 15);
		radioButtons.add(radio4);
		for (RadioButtons radiobutton : radioButtons) {
			this.add(radiobutton);
		}

		wifiDebug1 = new WifiDebugButtons(310, 160, 90, 30);
		wifiDebug1.addActionListener(new WifiDebug1Listener());
		wifiDebugButtons.add(wifiDebug1);
		wifiDebug2 = new WifiDebugButtons(520, 160, 90, 30);
		wifiDebug2.addActionListener(new WifiDebug2Listener());
		wifiDebugButtons.add(wifiDebug2);
		wifiDebug3 = new WifiDebugButtons(730, 160, 90, 30);
		wifiDebug3.addActionListener(new WifiDebug3Listener());
		wifiDebugButtons.add(wifiDebug3);
		wifiDebug4 = new WifiDebugButtons(940, 160, 90, 30);
		wifiDebug4.addActionListener(new WifiDebug4Listener());
		wifiDebugButtons.add(wifiDebug4);
		for (WifiDebugButtons wifiButton : wifiDebugButtons) {
			this.add(wifiButton);
		}

		for (String element : output1) {
			if (output1.size() > 0) {
				if (element.endsWith(":5555")) {
					wifiDebug1.setText("Disable WiFi");
				}
			}
			if (output1.size() > 1) {
				if (element.endsWith(":5555")) {
					wifiDebug2.setText("Disable WiFi");
				}
			}
			if (output1.size() > 2) {
				if (element.endsWith(":5555")) {
					wifiDebug3.setText("Disable WiFi");
				}
			}
			if (output1.size() > 3) {
				if (element.endsWith(":5555")) {
					wifiDebug4.setText("Disable WiFi");
				}
			}
		}

		installButton = new InstallButton(0, 300, 100, 50);
		installButton.addActionListener(new InstallButtonListener());
		this.add(installButton);

		uninstallAllButton = new UninstallAllButton(0, 200, 100, 50);
		uninstallAllButton.addActionListener(new UninstallAllButtonListener());
		this.add(uninstallAllButton);

		int j = 0;
		while (j < output1.size()) {
			if (command1.checkIfInstalled(output1.get(j))) {
				uninstallAllButton.setEnabled(true);
				break;
			}
			j++;
		}

		staticPane = new StaticPane();
		this.add(staticPane);

		fileTextField = new FileTextField();
		this.add(fileTextField);

		devicesButton = new DevicesButton(buttonIcon);
		devicesButton.addActionListener(new DevicesButtonListener());
		this.add(devicesButton);

		fileButton = new FileButton("Select Build");
		fileButton.addActionListener(new FileButtonListener());
		this.add(fileButton);
		
		progressBar = new ProgressBar();
		this.add(progressBar);

		this.setTitle("Adb Toolkit");
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setLayout(null);
		this.setResizable(false);
		this.setSize(1045, 420);
		this.setIconImage(frameIcon.getImage());

		for (int i = 0; i < output1.size(); i++) {
			
			if (i == 0) {
				
				radio1.setSelected(true);
				radio1.setVisible(true);
				switch (command1.getSafePathPackage(output1.get(0))) {
					case "com.smithmicro.tmobile.familymode.test" -> {
						labelIcon1.setIcon(logo_tmo);
						labelIcon1.setText("FamilyMode");
						labelIcon1.setVisible(true);
						uninstallApp1.setEnabled(true);
						uninstallApp1.setVisible(true);
						enableFirebase1.setEnabled(true);
						enableFirebase1.setVisible(true);
						saveLogsButton1.setEnabled(true);
					}
					case "com.smithmicro.safepath.family" -> {
						labelIcon1.setIcon(logo_product);
						labelIcon1.setText("SPFamily");
						labelIcon1.setVisible(true);
						uninstallApp1.setEnabled(true);
						uninstallApp1.setVisible(true);
						enableFirebase1.setEnabled(true);
						enableFirebase1.setVisible(true);
						saveLogsButton1.setEnabled(true);
					}
					case "com.smithmicro.att.securefamily" -> {
						labelIcon1.setIcon(logo_att);
						labelIcon1.setText("SecureFamily");
						labelIcon1.setVisible(true);
						uninstallApp1.setEnabled(true);
						uninstallApp1.setVisible(true);
						enableFirebase1.setEnabled(true);
						enableFirebase1.setVisible(true);
						saveLogsButton1.setEnabled(true);
					}
					case "" -> {
						labelIcon1.setIcon(notInstalled);
						labelIcon1.setText("Not Installed");
						labelIcon1.setVisible(true);
						uninstallApp1.setVisible(true);
						enableFirebase1.setVisible(true);
						saveLogsButton1.setEnabled(false);
					}
				}
				saveLogsButton1.setVisible(true);
				wifiDebug1.setText("WiFi Debug");
				if (output1.get(i).endsWith(":5555")) {
					wifiDebug1.setText("Disable WiFi");
				}
				wifiDebug1.setVisible(true);
				enableFirebase1.setVisible(true);
				reboot1.setVisible(true);
				device1TextPane.setText(output1.get(i) + "\n" + command1.getDeviceManufacturer(output1.get(i)) + "\n"
						+ command1.getDeviceModel(output1.get(i)) + "\n" + "Android "
						+ command1.getDeviceOSVersion(output1.get(i)) + "\n" + ips.get(i));
				device1TextPane.setVisible(true);
				takeScreenshotButton1.setVisible(true);

			} else if (i == 1) {

				radio2.setSelected(true);
				radio2.setVisible(true);
				switch (command1.getSafePathPackage(output1.get(1))) {
					case "com.smithmicro.tmobile.familymode.test" -> {
						labelIcon2.setIcon(logo_tmo);
						labelIcon2.setText("FamilyMode");
						labelIcon2.setVisible(true);
						uninstallApp2.setEnabled(true);
						uninstallApp2.setVisible(true);
						enableFirebase2.setEnabled(true);
						enableFirebase2.setVisible(true);
						saveLogsButton2.setEnabled(true);
					}
					case "com.smithmicro.safepath.family" -> {
						labelIcon2.setIcon(logo_product);
						labelIcon2.setText("SPFamily");
						labelIcon2.setVisible(true);
						uninstallApp2.setEnabled(true);
						uninstallApp2.setVisible(true);
						enableFirebase2.setEnabled(true);
						enableFirebase2.setVisible(true);
						saveLogsButton2.setEnabled(true);
					}
					case "com.smithmicro.att.securefamily" -> {
						labelIcon2.setIcon(logo_att);
						labelIcon2.setText("SecureFamily");
						labelIcon2.setVisible(true);
						uninstallApp2.setEnabled(true);
						uninstallApp2.setVisible(true);
						enableFirebase2.setEnabled(true);
						enableFirebase2.setVisible(true);
						saveLogsButton2.setEnabled(true);
					}
					case "" -> {
						labelIcon2.setIcon(notInstalled);
						labelIcon2.setText("Not Installed");
						labelIcon2.setVisible(true);
						uninstallApp2.setVisible(true);
						enableFirebase2.setVisible(true);
						saveLogsButton2.setEnabled(false);
					}
				}
				saveLogsButton2.setVisible(true);
				wifiDebug2.setText("WiFi Debug");
				if (output1.get(i).endsWith(":5555")) {
					wifiDebug2.setText("Disable WiFi");
				}
				wifiDebug2.setVisible(true);
				enableFirebase2.setVisible(true);
				reboot2.setVisible(true);
				device2TextPane.setText(output1.get(i) + "\n" + command1.getDeviceManufacturer(output1.get(i)) + "\n"
						+ command1.getDeviceModel(output1.get(i)) + "\n" + "Android "
						+ command1.getDeviceOSVersion(output1.get(i)) + "\n" + ips.get(i));
				device2TextPane.setVisible(true);
				takeScreenshotButton2.setVisible(true);

			} else if (i == 2) {

				radio3.setSelected(true);
				radio3.setVisible(true);
				switch (command1.getSafePathPackage(output1.get(2))) {
					case "com.smithmicro.tmobile.familymode.test" -> {
						labelIcon3.setIcon(logo_tmo);
						labelIcon3.setText("FamilyMode");
						labelIcon3.setVisible(true);
						uninstallApp3.setEnabled(true);
						uninstallApp3.setVisible(true);
						enableFirebase3.setEnabled(true);
						enableFirebase3.setVisible(true);
						saveLogsButton3.setEnabled(true);
					}
					case "com.smithmicro.safepath.family" -> {
						labelIcon3.setIcon(logo_product);
						labelIcon3.setText("SPFamily");
						labelIcon3.setVisible(true);
						uninstallApp3.setEnabled(true);
						uninstallApp3.setVisible(true);
						enableFirebase3.setEnabled(true);
						enableFirebase3.setVisible(true);
						saveLogsButton3.setEnabled(true);
					}
					case "com.smithmicro.att.securefamily" -> {
						labelIcon3.setIcon(logo_att);
						labelIcon3.setText("SecureFamily");
						labelIcon3.setVisible(true);
						uninstallApp3.setEnabled(true);
						uninstallApp3.setVisible(true);
						enableFirebase3.setEnabled(true);
						enableFirebase3.setVisible(true);
						saveLogsButton3.setEnabled(true);
					}
					case "" -> {
						labelIcon3.setIcon(notInstalled);
						labelIcon3.setText("Not Installed");
						labelIcon3.setVisible(true);
						uninstallApp3.setVisible(true);
						enableFirebase3.setVisible(true);
						saveLogsButton3.setEnabled(false);
					}
				}
				saveLogsButton3.setVisible(true);
				wifiDebug3.setText("WiFi Debug");
				if (output1.get(i).endsWith(":5555")) {
					wifiDebug3.setText("Disable WiFi");
				}
				wifiDebug3.setVisible(true);
				enableFirebase3.setVisible(true);
				reboot3.setVisible(true);
				device3TextPane.setText(output1.get(i) + "\n" + command1.getDeviceManufacturer(output1.get(i)) + "\n"
						+ command1.getDeviceModel(output1.get(i)) + "\n" + "Android "
						+ command1.getDeviceOSVersion(output1.get(i)) + "\n" + ips.get(i));
				device3TextPane.setVisible(true);
				takeScreenshotButton3.setVisible(true);

			} else if (i == 3) {

				radio4.setSelected(true);
				radio4.setVisible(true);
				switch (command1.getSafePathPackage(output1.get(3))) {
					case "com.smithmicro.tmobile.familymode.test" -> {
						labelIcon4.setIcon(logo_tmo);
						labelIcon4.setText("FamilyMode");
						labelIcon4.setVisible(true);
						uninstallApp4.setEnabled(true);
						uninstallApp4.setVisible(true);
						enableFirebase4.setEnabled(true);
						enableFirebase4.setVisible(true);
						saveLogsButton4.setEnabled(true);
					}
					case "com.smithmicro.safepath.family" -> {
						labelIcon4.setIcon(logo_product);
						labelIcon4.setText("SPFamily");
						labelIcon4.setVisible(true);
						uninstallApp4.setEnabled(true);
						uninstallApp4.setVisible(true);
						enableFirebase4.setEnabled(true);
						enableFirebase4.setVisible(true);
						saveLogsButton4.setEnabled(true);
					}
					case "com.smithmicro.att.securefamily" -> {
						labelIcon4.setIcon(logo_att);
						labelIcon4.setText("SecureFamily");
						labelIcon4.setVisible(true);
						uninstallApp4.setEnabled(true);
						uninstallApp4.setVisible(true);
						enableFirebase4.setEnabled(true);
						enableFirebase4.setVisible(true);
						saveLogsButton4.setEnabled(true);
					}
					case "" -> {
						labelIcon4.setIcon(notInstalled);
						labelIcon4.setText("Not Installed");
						labelIcon4.setVisible(true);
						uninstallApp4.setVisible(true);
						enableFirebase4.setVisible(true);
						saveLogsButton4.setEnabled(false);
					}
				}
				saveLogsButton4.setVisible(true);
				wifiDebug4.setText("WiFi Debug");
				if (output1.get(i).endsWith(":5555")) {
					wifiDebug4.setText("Disable WiFi");
				}
				wifiDebug4.setVisible(true);
				enableFirebase4.setVisible(true);
				reboot4.setVisible(true);
				device4TextPane.setText(output1.get(i) + "\n" + command1.getDeviceManufacturer(output1.get(i)) + "\n"
						+ command1.getDeviceModel(output1.get(i)) + "\n" + "Android "
						+ command1.getDeviceOSVersion(output1.get(i)) + "\n" + ips.get(i));
				device4TextPane.setVisible(true);
				takeScreenshotButton4.setVisible(true);
			}
		}
		this.setVisible(true);
	}
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			int progress = (Integer) evt.getNewValue();
			progressBar.setIndeterminate(false);
			progressBar.setValue(progress);
		}
	}
	class DevicesButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			for (DeviceTextPanes deviceTextPane : deviceTextPanes) {
				deviceTextPane.setVisible(false);
			}
			for (RadioButtons radiobutton : radioButtons) {
				radiobutton.setVisible(false);
			}
			for (SaveSPLogsButtons saveLogsButton : saveSPLogsButtons) {
				saveLogsButton.setVisible(false);
			}
			for (TakeScreenshotButtons screenshotButton : takeScreenshotButtons) {
				screenshotButton.setVisible(false);
			}
			for (WifiDebugButtons wifiButton : wifiDebugButtons) {
				wifiButton.setVisible(false);
			}
			for (EnableFirebaseButtons firebaseButton : enableFirebaseButtons) {
				firebaseButton.setVisible(false);
				firebaseButton.setEnabled(false);
			}
			for (UninstallAppButtons uninstallButton : uninstallAppButtons) {
				uninstallButton.setVisible(false);
				uninstallButton.setEnabled(false);
			}
			for (RebootButtons rebootButton : rebootButtons) {
				rebootButton.setVisible(false);
			}
			for (LogoIconLabels IconLabel : logoIconLabels) {
				IconLabel.setVisible(false);
			}
			output1.clear();
			ips.clear();
			output1 = command1.getConnectedDevices();
			for (String element : output1) {
				ips.add(command1.getWlanIp(element));
			}

			for (int i = 0; i < output1.size(); i++) {

				if (i == 0) {

					radio1.setSelected(true);
					radio1.setVisible(true);
					switch (command1.getSafePathPackage(output1.get(0))) {
						case "com.smithmicro.tmobile.familymode.test" -> {
							labelIcon1.setIcon(logo_tmo);
							labelIcon1.setText("FamilyMode");
							labelIcon1.setVisible(true);
							uninstallApp1.setEnabled(true);
							uninstallApp1.setVisible(true);
							enableFirebase1.setEnabled(true);
							enableFirebase1.setVisible(true);
							saveLogsButton1.setEnabled(true);
						}
						case "com.smithmicro.safepath.family" -> {
							labelIcon1.setIcon(logo_product);
							labelIcon1.setText("SPFamily");
							labelIcon1.setVisible(true);
							uninstallApp1.setEnabled(true);
							uninstallApp1.setVisible(true);
							enableFirebase1.setEnabled(true);
							enableFirebase1.setVisible(true);
							saveLogsButton1.setEnabled(true);
						}
						case "com.smithmicro.att.securefamily" -> {
							labelIcon1.setIcon(logo_att);
							labelIcon1.setText("SecureFamily");
							labelIcon1.setVisible(true);
							uninstallApp1.setEnabled(true);
							uninstallApp1.setVisible(true);
							enableFirebase1.setEnabled(true);
							enableFirebase1.setVisible(true);
							saveLogsButton1.setEnabled(true);
						}
						case "" -> {
							labelIcon1.setIcon(notInstalled);
							labelIcon1.setText("Not Installed");
							labelIcon1.setVisible(true);
							uninstallApp1.setVisible(true);
							enableFirebase1.setVisible(true);
							saveLogsButton1.setEnabled(false);
						}
					}
					saveLogsButton1.setVisible(true);
					wifiDebug1.setText("WiFi Debug");
					if (output1.get(i).endsWith(":5555")) {
						wifiDebug1.setText("Disable WiFi");
					}
					wifiDebug1.setVisible(true);
					enableFirebase1.setVisible(true);
					reboot1.setVisible(true);
					device1TextPane.setText(output1.get(i) + "\n" + command1.getDeviceManufacturer(output1.get(i))
							+ "\n" + command1.getDeviceModel(output1.get(i)) + "\n" + "Android "
							+ command1.getDeviceOSVersion(output1.get(i)) + "\n" + ips.get(i));
					device1TextPane.setVisible(true);
					takeScreenshotButton1.setVisible(true);

				} else if (i == 1) {

					radio2.setSelected(true);
					radio2.setVisible(true);
					switch (command1.getSafePathPackage(output1.get(1))) {
						case "com.smithmicro.tmobile.familymode.test" -> {
							labelIcon2.setIcon(logo_tmo);
							labelIcon2.setText("FamilyMode");
							labelIcon2.setVisible(true);
							uninstallApp2.setEnabled(true);
							uninstallApp2.setVisible(true);
							enableFirebase2.setEnabled(true);
							enableFirebase2.setVisible(true);
							saveLogsButton2.setEnabled(true);
						}
						case "com.smithmicro.safepath.family" -> {
							labelIcon2.setIcon(logo_product);
							labelIcon2.setText("SPFamily");
							labelIcon2.setVisible(true);
							uninstallApp2.setEnabled(true);
							uninstallApp2.setVisible(true);
							enableFirebase2.setEnabled(true);
							enableFirebase2.setVisible(true);
							saveLogsButton2.setEnabled(true);
						}
						case "com.smithmicro.att.securefamily" -> {
							labelIcon2.setIcon(logo_att);
							labelIcon2.setText("SecureFamily");
							labelIcon2.setVisible(true);
							uninstallApp2.setEnabled(true);
							uninstallApp2.setVisible(true);
							enableFirebase2.setEnabled(true);
							enableFirebase2.setVisible(true);
							saveLogsButton2.setEnabled(true);
						}
						case "" -> {
							labelIcon2.setIcon(notInstalled);
							labelIcon2.setText("Not Installed");
							labelIcon2.setVisible(true);
							uninstallApp2.setVisible(true);
							enableFirebase2.setVisible(true);
							saveLogsButton2.setEnabled(false);
						}
					}
					saveLogsButton2.setVisible(true);
					wifiDebug2.setText("WiFi Debug");
					if (output1.get(i).endsWith(":5555")) {
						wifiDebug2.setText("Disable WiFi");
					}
					wifiDebug2.setVisible(true);
					enableFirebase2.setVisible(true);
					reboot2.setVisible(true);
					device2TextPane.setText(output1.get(i) + "\n" + command1.getDeviceManufacturer(output1.get(i))
							+ "\n" + command1.getDeviceModel(output1.get(i)) + "\n" + "Android "
							+ command1.getDeviceOSVersion(output1.get(i)) + "\n" + ips.get(i));
					device2TextPane.setVisible(true);
					takeScreenshotButton2.setVisible(true);

				} else if (i == 2) {

					radio3.setSelected(true);
					radio3.setVisible(true);
					switch (command1.getSafePathPackage(output1.get(2))) {
						case "com.smithmicro.tmobile.familymode.test" -> {
							labelIcon3.setIcon(logo_tmo);
							labelIcon3.setText("FamilyMode");
							labelIcon3.setVisible(true);
							uninstallApp3.setEnabled(true);
							uninstallApp3.setVisible(true);
							enableFirebase3.setEnabled(true);
							enableFirebase3.setVisible(true);
							saveLogsButton3.setEnabled(true);
						}
						case "com.smithmicro.safepath.family" -> {
							labelIcon3.setIcon(logo_product);
							labelIcon3.setText("SPFamily");
							labelIcon3.setVisible(true);
							uninstallApp3.setEnabled(true);
							uninstallApp3.setVisible(true);
							enableFirebase3.setEnabled(true);
							enableFirebase3.setVisible(true);
							saveLogsButton3.setEnabled(true);
						}
						case "com.smithmicro.att.securefamily" -> {
							labelIcon3.setIcon(logo_att);
							labelIcon3.setText("SecureFamily");
							labelIcon3.setVisible(true);
							uninstallApp3.setEnabled(true);
							uninstallApp3.setVisible(true);
							enableFirebase3.setEnabled(true);
							enableFirebase3.setVisible(true);
							saveLogsButton3.setEnabled(true);
						}
						case "" -> {
							labelIcon3.setIcon(notInstalled);
							labelIcon3.setText("Not Installed");
							labelIcon3.setVisible(true);
							uninstallApp3.setVisible(true);
							enableFirebase3.setVisible(true);
							saveLogsButton3.setEnabled(false);
						}
					}
					wifiDebug3.setText("WiFi Debug");
					if (output1.get(i).endsWith(":5555")) {
						wifiDebug3.setText("Disable WiFi");
					}
					saveLogsButton3.setVisible(true);
					wifiDebug3.setVisible(true);
					enableFirebase3.setVisible(true);
					reboot3.setVisible(true);
					device3TextPane.setText(output1.get(i) + "\n" + command1.getDeviceManufacturer(output1.get(i))
							+ "\n" + command1.getDeviceModel(output1.get(i)) + "\n" + "Android "
							+ command1.getDeviceOSVersion(output1.get(i)) + "\n" + ips.get(i));
					device3TextPane.setVisible(true);
					takeScreenshotButton3.setVisible(true);

				} else if (i == 3) {

					radio4.setSelected(true);
					radio4.setVisible(true);
					switch (command1.getSafePathPackage(output1.get(3))) {
						case "com.smithmicro.tmobile.familymode.test" -> {
							labelIcon4.setIcon(logo_tmo);
							labelIcon4.setText("FamilyMode");
							labelIcon4.setVisible(true);
							uninstallApp4.setEnabled(true);
							uninstallApp4.setVisible(true);
							enableFirebase4.setEnabled(true);
							enableFirebase4.setVisible(true);
							saveLogsButton4.setEnabled(true);
						}
						case "com.smithmicro.safepath.family" -> {
							labelIcon4.setIcon(logo_product);
							labelIcon4.setText("SPFamily");
							labelIcon4.setVisible(true);
							uninstallApp4.setEnabled(true);
							uninstallApp4.setVisible(true);
							enableFirebase4.setEnabled(true);
							enableFirebase4.setVisible(true);
							saveLogsButton4.setEnabled(true);
						}
						case "com.smithmicro.att.securefamily" -> {
							labelIcon4.setIcon(logo_att);
							labelIcon4.setText("SecureFamily");
							labelIcon4.setVisible(true);
							uninstallApp4.setEnabled(true);
							uninstallApp4.setVisible(true);
							enableFirebase4.setEnabled(true);
							enableFirebase4.setVisible(true);
							saveLogsButton4.setEnabled(true);
						}
						case "" -> {
							labelIcon4.setIcon(notInstalled);
							labelIcon4.setText("Not Installed");
							labelIcon4.setVisible(true);
							uninstallApp4.setVisible(true);
							enableFirebase4.setVisible(true);
							saveLogsButton4.setEnabled(false);
						}
					}
					saveLogsButton4.setVisible(true);
					wifiDebug4.setText("WiFi Debug");
					if (output1.get(i).endsWith(":5555")) {
						wifiDebug4.setText("Disable WiFi");
					}
					wifiDebug4.setVisible(true);
					enableFirebase4.setVisible(true);
					reboot4.setVisible(true);
					device4TextPane.setText(output1.get(i) + "\n" + command1.getDeviceManufacturer(output1.get(i))
							+ "\n" + command1.getDeviceModel(output1.get(i)) + "\n" + "Android "
							+ command1.getDeviceOSVersion(output1.get(i)) + "\n" + ips.get(i));
					device4TextPane.setVisible(true);
					takeScreenshotButton4.setVisible(true);
				}
			}
		}
	}

	class InstallButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			output1 = command1.getConnectedDevices();
			progressBar.setString("Installing...");
			progressBar.setIndeterminate(true);
			progressBar.setBackground(new Color(238, 238, 238));
			installButton.setEnabled(false);

			if (radio1.isSelected() && output1.size() > 0) {
				radio1State = true;
				Task task1 = new Task("adb -s " + output1.get(0) + " install " + "\"" + file.getAbsolutePath() + "\"");
				task1.addPropertyChangeListener(null);
				task1.execute();
			} else {
				radio1State = false;
			}

			if (radio2.isSelected() && output1.size() > 1) {
				radio2State = true;
				Task task2 = new Task("adb -s " + output1.get(1) + " install " + "\"" + file.getAbsolutePath() + "\"");
				task2.addPropertyChangeListener(null);
				task2.execute();
			} else {
				radio2State = false;
			}
			if (radio3.isSelected() && output1.size() > 2) {
				radio3State = true;
				Task task3 = new Task("adb -s " + output1.get(2) + " install " + "\"" + file.getAbsolutePath() + "\"");
				task3.addPropertyChangeListener(null);
				task3.execute();
			} else {
				radio3State = false;
			}
			if (radio4.isSelected() && output1.size() > 3) {
				radio4State = true;
				Task task4 = new Task("adb -s " + output1.get(3) + " install " + "\"" + file.getAbsolutePath() + "\"");
				task4.addPropertyChangeListener(null);
				task4.execute();
			} else {
				radio1State = false;
			}
			uninstallAllButton.setEnabled(true);
		}
	}
	class UninstallAllButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			output1 = command1.getConnectedDevices();
			progressBar.setString("Uninstalling...");
			progressBar.setIndeterminate(true);
			progressBar.setBackground(new Color(238, 238, 238));
			uninstallAllButton.setEnabled(false);
			int i = 0;
			Task[] task = new Task[4];
			while (i < output1.size()) {
				task[i] = new Task("adb -s " + output1.get(i) + " shell pm uninstall "
						+ command1.getSafePathPackage(output1.get(i)));
				task[i].addPropertyChangeListener(null);
				task[i].execute();
				i++;
			}
		}
	}
	class FileButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser(
					"C:\\Users\\dtomic\\OneDrive - Smith Micro Software\\SP7\\Master builds\\");
			int response = fileChooser.showOpenDialog(null);
			if (response == JFileChooser.APPROVE_OPTION) {
				file = new File(fileChooser.getSelectedFile().getAbsolutePath());
				fileTextField.setText(file.getName());
				if (radio1.isSelected() || radio2.isSelected() || radio3.isSelected() || radio4.isSelected()) {
					installButton.setEnabled(true);
				}
				progressBar.setBackground(new Color(238, 238, 238));
				progressBar.setString("Waiting for build...");
			}
		}
	}
	class SaveLogsButton1Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser("C:/AdbToolkit/logs");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int response = fileChooser.showSaveDialog(null);
			if (response == JFileChooser.APPROVE_OPTION) {
				file = new File(fileChooser.getSelectedFile().getAbsolutePath());
				output1 = command1.getConnectedDevices();
				command1.saveLogs(output1.get(0), file.getAbsolutePath());
				JOptionPane.showMessageDialog(null, "Safe Path logs saved!", "Safe Path Logs",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
	class SaveLogsButton2Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser("C:/AdbToolkit/logs");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int response = fileChooser.showSaveDialog(null);
			if (response == JFileChooser.APPROVE_OPTION) {
				file = new File(fileChooser.getSelectedFile().getAbsolutePath());
				output1 = command1.getConnectedDevices();
				command1.saveLogs(output1.get(1), file.getAbsolutePath());
				JOptionPane.showMessageDialog(null, "Safe Path logs saved!", "Safe Path Logs",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
	class SaveLogsButton3Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser("C:/AdbToolkit/logs");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int response = fileChooser.showSaveDialog(null);
			if (response == JFileChooser.APPROVE_OPTION) {
				file = new File(fileChooser.getSelectedFile().getAbsolutePath());
				output1 = command1.getConnectedDevices();
				command1.saveLogs(output1.get(2), file.getAbsolutePath());
				JOptionPane.showMessageDialog(null, "Safe Path logs saved!", "Safe Path Logs",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
	class SaveLogsButton4Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser("C:/AdbToolkit/logs");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int response = fileChooser.showSaveDialog(null);
			if (response == JFileChooser.APPROVE_OPTION) {
				file = new File(fileChooser.getSelectedFile().getAbsolutePath());
				output1 = command1.getConnectedDevices();
				command1.saveLogs(output1.get(3), file.getAbsolutePath());
				JOptionPane.showMessageDialog(null, "Safe Path logs saved!", "Safe Path Logs",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
	class TakeScreenshotButton1Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			output1 = command1.getConnectedDevices();
			String output = command1.takeScreenshot(output1.get(0), "sdcard/", "screenshot.png");
			File device1 = new File("C:/AdbToolkit/Screenshots/Device1");
			if (!device1.exists()) {
				device1.mkdirs();
			}
			command1.pullFile(output1.get(0), output, "C:/AdbToolkit/Screenshots/Device1");
			JOptionPane.showMessageDialog(null,
					"Device1 screenshot captured!" + "\n" + "Location: C:/AdbToolkit/Screenshots/Device1",
					"Screenshot Capture", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	class TakeScreenshotButton2Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			output1 = command1.getConnectedDevices();
			String output = command1.takeScreenshot(output1.get(1), "sdcard/", "screenshot.png");
			File device2 = new File("C:/AdbToolkit/Screenshots/Device2");
			if (!device2.exists()) {
				device2.mkdirs();
			}
			command1.pullFile(output1.get(1), output, "C:/AdbToolkit/Screenshots/Device2");
			JOptionPane.showMessageDialog(null,
					"Device2 screenshot captured!" + "\n" + "Location: C:/AdbToolkit/Screenshots/Device2",
					"Screenshot Capture", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	class TakeScreenshotButton3Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			output1 = command1.getConnectedDevices();
			String output = command1.takeScreenshot(output1.get(2), "sdcard/", "screenshot.png");
			File device3 = new File("C:/AdbToolkit/Screenshots/Device3");
			if (!device3.exists()) {
				device3.mkdirs();
			}
			command1.pullFile(output1.get(2), output, "C:/AdbToolkit/Screenshots/Device3");
			JOptionPane.showMessageDialog(null,
					"Device3 screenshot captured!" + "\n" + "Location: C:/AdbToolkit_Screenshots/Device3",
					"Screenshot Capture", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	class TakeScreenshotButton4Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			output1 = command1.getConnectedDevices();
			String output = command1.takeScreenshot(output1.get(3), "sdcard/", "screenshot.png");
			File device4 = new File("C:/AdbToolkit/Screenshots/Device4");
			if (!device4.exists()) {
				device4.mkdirs();
			}
			command1.pullFile(output1.get(3), output, "C:/AdbToolkit/Screenshots/Device4");
			JOptionPane.showMessageDialog(null,
					"Device4 screenshot captured!" + "\n" + "Location: C:/AdbToolkit/Screenshots/Device4",
					"Screenshot Capture", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	class WifiDebug1Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!output1.get(0).endsWith(":5555")) {
				command1.startWifiDebugging(output1.get(0), ips.get(0));
				JOptionPane.showMessageDialog(null,
						"""
								Debugging over WiFi is enabled!
								If prompted on the device, allow wireless debugging on specific wifi network.
								You may disconnect USB cable from this device.""",
						"Enable WiFi Debugging.", JOptionPane.INFORMATION_MESSAGE);
				wifiDebug4.setText("Disable WiFi");
				devicesButton.doClick(200);
			} else {
				command1.stopWifiDebugging(output1.get(0), ips.get(0));
				JOptionPane.showMessageDialog(null, "Debugging over WiFi is disabled!", "Disable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				devicesButton.doClick(100);
			}
		}
	}
	class WifiDebug2Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!output1.get(1).endsWith(":5555")) {
				command1.startWifiDebugging(output1.get(1), ips.get(1));
				JOptionPane.showMessageDialog(null,
						"""
								Debugging over WiFi is enabled!
								If prompted on the device, allow wireless debugging on specific wifi network.
								You may disconnect USB cable from this device.""",
						"Enable WiFi Debugging.", JOptionPane.INFORMATION_MESSAGE);
				wifiDebug4.setText("Disable WiFi");
				devicesButton.doClick(200);
			} else {
				command1.stopWifiDebugging(output1.get(1), ips.get(1));
				JOptionPane.showMessageDialog(null, "Debugging over WiFi is disabled!", "Disable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				devicesButton.doClick(100);
			}
		}
	}
	class WifiDebug3Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!output1.get(2).endsWith(":5555")) {
				command1.startWifiDebugging(output1.get(2), ips.get(2));
				JOptionPane.showMessageDialog(null,
						"""
								Debugging over WiFi is enabled!
								If prompted on the device, allow wireless debugging on specific wifi network.
								You may disconnect USB cable from this device.""",
						"Enable WiFi Debugging.", JOptionPane.INFORMATION_MESSAGE);
				wifiDebug4.setText("Disable WiFi");
				devicesButton.doClick(200);
			} else {
				command1.stopWifiDebugging(output1.get(2), ips.get(2));
				JOptionPane.showMessageDialog(null, "Debugging over WiFi is disabled!", "Disable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				devicesButton.doClick(100);
			}
		}
	}
	class WifiDebug4Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println(output1);
			if (!output1.get(3).endsWith(":5555")) {
				command1.startWifiDebugging(output1.get(3), ips.get(3));
				JOptionPane.showMessageDialog(null,
						"""
								Debugging over WiFi is enabled!
								If prompted on the device, allow wireless debugging on specific wifi network.
								You may disconnect USB cable from this device.""",
						"Enable WiFi Debugging.", JOptionPane.INFORMATION_MESSAGE);
				wifiDebug4.setText("Disable WiFi");
				devicesButton.doClick(200);
			} else {
				command1.stopWifiDebugging(output1.get(3), ips.get(3));
				JOptionPane.showMessageDialog(null, "Debugging over WiFi is disabled!", "Disable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				devicesButton.doClick(100);
			}
		}
	}
	class EnableFirebase1Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			output1 = command1.getConnectedDevices();
			command1.enableAnalyticsDebug(output1.get(0), command1.getSafePathPackage(output1.get(0)));
			if (command1.checkIfInstalled(output1.get(0))) {
				JOptionPane.showMessageDialog(null,
						"Firebase Debugging enabled!" + "\n"
								+ "Make sure 'Logging Analytics Events' toggle button is also enabled in Debug menu.",
						"Enable Firebase Debugging", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
	class EnableFirebase2Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			output1 = command1.getConnectedDevices();
			command1.enableAnalyticsDebug(output1.get(1), command1.getSafePathPackage(output1.get(1)));
			if (command1.checkIfInstalled(output1.get(1))) {
				JOptionPane.showMessageDialog(null,
						"Firebase Debugging enabled!" + "\n"
								+ "Make sure 'Logging Analytics Events' toggle button is also enabled in Debug menu.",
						"Enable Firebase Debugging", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
	class EnableFirebase3Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			output1 = command1.getConnectedDevices();
			command1.enableAnalyticsDebug(output1.get(2), command1.getSafePathPackage(output1.get(2)));
			if (command1.checkIfInstalled(output1.get(2))) {
				JOptionPane.showMessageDialog(null,
						"Firebase Debugging enabled!" + "\n"
								+ "Make sure 'Logging Analytics Events' toggle button is also enabled in Debug menu.",
						"Enable Firebase Debugging", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
	class EnableFirebase4Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			output1 = command1.getConnectedDevices();
			command1.enableAnalyticsDebug(output1.get(3), command1.getSafePathPackage(output1.get(3)));
			if (command1.checkIfInstalled(output1.get(3))) {
				JOptionPane.showMessageDialog(null,
						"Firebase Debugging enabled!" + "\n"
								+ "Make sure 'Logging Analytics Events' toggle button is also enabled in Debug menu.",
						"Enable Firebase Debugging", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
	class Reboot1Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			command1.reboot(output1.get(0));
		}
	}
	class Reboot2Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			command1.reboot(output1.get(1));
		}
	}
	class Reboot3Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			command1.reboot(output1.get(2));
		}
	}
	class Reboot4Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			command1.reboot(output1.get(3));
		}
	}
	class UninstallApp1Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			command1.uninstallApp(output1.get(0), command1.getSafePathPackage(output1.get(0)));
			JOptionPane.showMessageDialog(null, "App is uninstalled!", "Enable WiFi Debugging.",
					JOptionPane.INFORMATION_MESSAGE);
			saveLogsButton1.setEnabled(false);
			devicesButton.doClick();
		}
	}
	class UninstallApp2Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			command1.uninstallApp(output1.get(1), command1.getSafePathPackage(output1.get(1)));
			JOptionPane.showMessageDialog(null, "App is uninstalled!", "Enable WiFi Debugging.",
					JOptionPane.INFORMATION_MESSAGE);
			saveLogsButton2.setEnabled(false);
			devicesButton.doClick();
		}
	}
	class UninstallApp3Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			command1.uninstallApp(output1.get(2), command1.getSafePathPackage(output1.get(2)));
			JOptionPane.showMessageDialog(null, "App is uninstalled!", "Enable WiFi Debugging.",
					JOptionPane.INFORMATION_MESSAGE);
			saveLogsButton3.setEnabled(false);
			devicesButton.doClick();
		}
	}
	class UninstallApp4Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			command1.uninstallApp(output1.get(3), command1.getSafePathPackage(output1.get(3)));
			JOptionPane.showMessageDialog(null, "App is uninstalled!", "Enable WiFi Debugging.",
					JOptionPane.INFORMATION_MESSAGE);
			saveLogsButton4.setEnabled(false);
			devicesButton.doClick();
		}
	}
	// Thread synchronization to determine the end of all processes
	public void increaseCounter() {
		synchronized (lock) {
			counter++;
		}
	}
	public void decreaseCounter() {
		synchronized (lock) {
			counter--;
		}
	}
	class Task extends SwingWorker<Void, Void> {
		private final String command;

		public Task(String command) {
			this.command = command;
		}

		@Override
		public Void doInBackground() {
			increaseCounter();
			command1.runCommand(command);
			return null;
		}
		@Override
		public void done() {
			decreaseCounter();
			if (counter == 0) {
				progressBar.setString("Done!");
				progressBar.setBackground(Color.green);
				if (!Objects.equals(fileTextField.getText(), "")) {
					installButton.setEnabled(true);
				}
				progressBar.setIndeterminate(false);
				devicesButton.doClick();
			}
		}
	}
}