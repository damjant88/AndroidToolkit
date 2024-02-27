import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import javax.swing.*;
import Buttons.*;

public class MyFrame extends JFrame implements PropertyChangeListener {

	Util utility = new Util();
	ArrayList<String> listOfDevices = utility.getConnectedDevices();
	private static int counter = 0;
	private static final Object lock = new Object();
	ArrayList<String> ips = new ArrayList<>();
	ImageIcon buttonIcon, logo_tmo, logo_att, logo_product, frameIcon, notInstalled, logo_sprint, logo_orange;
	File file1 = null;
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
		logo_sprint = new ImageIcon(getToolkit().getImage(ClassLoader.getSystemResource("sprint.png")));
		logo_orange = new ImageIcon(getToolkit().getImage(ClassLoader.getSystemResource("toyo.png")));

		// Get list of wlan0 IP addresses
		for (String element : listOfDevices) {
			ips.add(utility.getWlanIp(element));
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

		Image image6 = logo_sprint.getImage();
		newimg = image6.getScaledInstance(45, 45, java.awt.Image.SCALE_SMOOTH);
		logo_sprint = new ImageIcon(newimg);

		Image image7 = logo_orange.getImage();
		newimg = image7.getScaledInstance(45, 45, java.awt.Image.SCALE_SMOOTH);
		logo_orange = new ImageIcon(newimg);

		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu editMenu = new JMenu("Edit");
		JMenu helpMenu = new JMenu("Help");
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(helpMenu);
		JMenuItem defaultBuildLocation = new JMenuItem("Set Default 'Select Build' Location");
		editMenu.add(defaultBuildLocation);
		defaultBuildLocation.addActionListener(new DefaultBuildLocationListener());
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

		for (String element : listOfDevices) {
			if (listOfDevices.size() > 0) {
				if (element.endsWith(":5555")) {
					wifiDebug1.setText("Disable WiFi");
				}
			}
			if (listOfDevices.size() > 1) {
				if (element.endsWith(":5555")) {
					wifiDebug2.setText("Disable WiFi");
				}
			}
			if (listOfDevices.size() > 2) {
				if (element.endsWith(":5555")) {
					wifiDebug3.setText("Disable WiFi");
				}
			}
			if (listOfDevices.size() > 3) {
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
		while (j < listOfDevices.size()) {
			if (utility.checkIfInstalled(listOfDevices.get(j))) {
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
		for (int i = 0; i < listOfDevices.size(); i++) {
			if (i == 0) {
				setIconAndButtons1(i);
			} else if (i == 1) {
				setIconAndButtons2(i);
			} else if (i == 2) {
				setIconAndButtons3(i);
			} else if (i == 3) {
				setIconAndButtons4(i);
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
	class DefaultBuildLocationListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int response = fileChooser.showOpenDialog(null);
			File file2 = null;
			if (response == JFileChooser.APPROVE_OPTION) {
				file2 = new File(fileChooser.getSelectedFile().getAbsolutePath());
				String defaultPath = file2.getAbsolutePath();
			}
			try {
				FileOutputStream fs = new FileOutputStream("C:/AdbToolkit/location.ser");
				ObjectOutputStream os = new ObjectOutputStream(fs);
				os.writeObject(file2);
				os.close();
				JOptionPane.showMessageDialog(null, "Default build location is set!" + "\n" + file2.getAbsolutePath(), "Default Build Location",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

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
			listOfDevices.clear();
			ips.clear();
			listOfDevices = utility.getConnectedDevices();
			for (String element : listOfDevices) {
				ips.add(utility.getWlanIp(element));
			}

			for (int i = 0; i < listOfDevices.size(); i++) {
				if (i == 0) {
					setIconAndButtons1(i);
				} else if (i == 1) {
					setIconAndButtons2(i);
				} else if (i == 2) {
					setIconAndButtons3(i);
				} else if (i == 3) {
					setIconAndButtons4(i);
				}
			}
		}
	}
	private void setIconAndButtons4(int i) {
		radio4.setSelected(true);
		radio4.setVisible(true);
		String packageName = utility.getSafePathPackage(listOfDevices.get(i));
		switch (packageName) {
			case "com.smithmicro.tmobile.familymode.test":
			case "com.tmobile.familycontrols": {
				labelIcon4.setIcon(logo_tmo);
				labelIcon4.setText("FamilyMode");
				labelIcon4.setVisible(true);
				uninstallApp4.setEnabled(true);
				uninstallApp4.setVisible(true);
				enableFirebase4.setEnabled(true);
				enableFirebase4.setVisible(true);
				saveLogsButton4.setEnabled(true);
				break;
			}
			case "com.smithmicro.safepath.family":
			case "com.smithmicro.safepath.family.child": {
				labelIcon4.setIcon(logo_product);
				labelIcon4.setText("SPFamily");
				labelIcon4.setVisible(true);
				uninstallApp4.setEnabled(true);
				uninstallApp4.setVisible(true);
				enableFirebase4.setEnabled(true);
				enableFirebase4.setVisible(true);
				saveLogsButton4.setEnabled(true);
				break;
			}
			case "com.smithmicro.att.securefamily":
			case "com.att.securefamilycompanion": {
				labelIcon4.setIcon(logo_att);
				labelIcon4.setText("SecureFamily");
				labelIcon4.setVisible(true);
				uninstallApp4.setEnabled(true);
				uninstallApp4.setVisible(true);
				enableFirebase4.setEnabled(true);
				enableFirebase4.setVisible(true);
				saveLogsButton4.setEnabled(true);
				break;
			}
			case "com.smithmicro.sprint.safeandfound.test":
			case "com.sprint.safefound": {
				labelIcon4.setIcon(logo_sprint);
				labelIcon4.setText("Safe&Found");
				labelIcon4.setVisible(true);
				uninstallApp4.setEnabled(true);
				uninstallApp4.setVisible(true);
				enableFirebase4.setEnabled(true);
				enableFirebase4.setVisible(true);
				saveLogsButton4.setEnabled(true);
				break;
			}
			case "com.smithmicro.orangespain.test": {
				labelIcon4.setIcon(logo_orange);
				labelIcon4.setText("TuYo");
				labelIcon4.setVisible(true);
				uninstallApp4.setEnabled(true);
				uninstallApp4.setVisible(true);
				enableFirebase4.setEnabled(true);
				enableFirebase4.setVisible(true);
				saveLogsButton4.setEnabled(true);
				break;
			}
			case "": {
				labelIcon4.setIcon(notInstalled);
				labelIcon4.setText("Not Installed");
				labelIcon4.setVisible(true);
				uninstallApp4.setVisible(true);
				enableFirebase4.setVisible(true);
				saveLogsButton4.setEnabled(false);
				break;
			}
		}
		saveLogsButton4.setVisible(true);
		wifiDebug4.setText("WiFi Debug");
		if (listOfDevices.get(i).endsWith(":5555")) {
			wifiDebug4.setText("Disable WiFi");
		}
		wifiDebug4.setVisible(true);
		enableFirebase4.setVisible(true);
		reboot4.setVisible(true);
		device4TextPane.setText(listOfDevices.get(i) + "\n" + utility.getDeviceManufacturer(listOfDevices.get(i)) + "\n"
				+ utility.getDeviceModel(listOfDevices.get(i)) + "\n" + "Android "
				+ utility.getDeviceOSVersion(listOfDevices.get(i)) + "\n" + ips.get(i));
		device4TextPane.setVisible(true);
		takeScreenshotButton4.setVisible(true);
		int j = 0;
		while (j < listOfDevices.size()) {
			if (utility.checkIfInstalled(listOfDevices.get(j))) {
				uninstallAllButton.setEnabled(true);
				break;
			}
			j++;
		}
	}

	private void setIconAndButtons3(int i) {
		radio3.setSelected(true);
		radio3.setVisible(true);
		String packageName3 = utility.getSafePathPackage(listOfDevices.get(i));
		switch (packageName3) {
			case "com.smithmicro.tmobile.familymode.test":
			case "com.tmobile.familycontrols": {
				labelIcon3.setIcon(logo_tmo);
				labelIcon3.setText("FamilyMode");
				labelIcon3.setVisible(true);
				uninstallApp3.setEnabled(true);
				uninstallApp3.setVisible(true);
				enableFirebase3.setEnabled(true);
				enableFirebase3.setVisible(true);
				saveLogsButton3.setEnabled(true);
				break;
			}
			case "com.smithmicro.safepath.family":
			case "com.smithmicro.safepath.family.child": {
				labelIcon3.setIcon(logo_product);
				labelIcon3.setText("SPFamily");
				labelIcon3.setVisible(true);
				uninstallApp3.setEnabled(true);
				uninstallApp3.setVisible(true);
				enableFirebase3.setEnabled(true);
				enableFirebase3.setVisible(true);
				saveLogsButton3.setEnabled(true);
				break;
			}
			case "com.smithmicro.att.securefamily":
			case "com.att.securefamilycompanion": {
				labelIcon3.setIcon(logo_att);
				labelIcon3.setText("SecureFamily");
				labelIcon3.setVisible(true);
				uninstallApp3.setEnabled(true);
				uninstallApp3.setVisible(true);
				enableFirebase3.setEnabled(true);
				enableFirebase3.setVisible(true);
				saveLogsButton3.setEnabled(true);
				break;
			}
			case "com.smithmicro.sprint.safeandfound.test":
			case "com.sprint.safefound": {
				labelIcon3.setIcon(logo_sprint);
				labelIcon3.setText("Safe&Found");
				labelIcon3.setVisible(true);
				uninstallApp3.setEnabled(true);
				uninstallApp3.setVisible(true);
				enableFirebase3.setEnabled(true);
				enableFirebase3.setVisible(true);
				saveLogsButton3.setEnabled(true);
				break;
			}
			case "com.smithmicro.orangespain.test": {
				labelIcon3.setIcon(logo_orange);
				labelIcon3.setText("TuYo");
				labelIcon3.setVisible(true);
				uninstallApp3.setEnabled(true);
				uninstallApp3.setVisible(true);
				enableFirebase3.setEnabled(true);
				enableFirebase3.setVisible(true);
				saveLogsButton3.setEnabled(true);
				break;
			}
			case "": {
				labelIcon3.setIcon(notInstalled);
				labelIcon3.setText("Not Installed");
				labelIcon3.setVisible(true);
				uninstallApp3.setVisible(true);
				enableFirebase3.setVisible(true);
				saveLogsButton3.setEnabled(false);
				break;
			}
		}
		saveLogsButton3.setVisible(true);
		wifiDebug3.setText("WiFi Debug");
		if (listOfDevices.get(i).endsWith(":5555")) {
			wifiDebug3.setText("Disable WiFi");
		}
		wifiDebug3.setVisible(true);
		enableFirebase3.setVisible(true);
		reboot3.setVisible(true);
		device3TextPane.setText(listOfDevices.get(i) + "\n" + utility.getDeviceManufacturer(listOfDevices.get(i)) + "\n"
				+ utility.getDeviceModel(listOfDevices.get(i)) + "\n" + "Android "
				+ utility.getDeviceOSVersion(listOfDevices.get(i)) + "\n" + ips.get(i));
		device3TextPane.setVisible(true);
		takeScreenshotButton3.setVisible(true);
		int j = 0;
		while (j < listOfDevices.size()) {
			if (utility.checkIfInstalled(listOfDevices.get(j))) {
				uninstallAllButton.setEnabled(true);
				break;
			}
			j++;
		}
	}

	private void setIconAndButtons2(int i) {
		radio2.setSelected(true);
		radio2.setVisible(true);
		String packageName2 = utility.getSafePathPackage(listOfDevices.get(i));
		switch (packageName2) {
			case "com.smithmicro.tmobile.familymode.test":
			case "com.tmobile.familycontrols": {
				labelIcon2.setIcon(logo_tmo);
				labelIcon2.setText("FamilyMode");
				labelIcon2.setVisible(true);
				uninstallApp2.setEnabled(true);
				uninstallApp2.setVisible(true);
				enableFirebase2.setEnabled(true);
				enableFirebase2.setVisible(true);
				saveLogsButton2.setEnabled(true);
				break;
			}
			case "com.smithmicro.safepath.family":
			case "com.smithmicro.safepath.family.child": {
				labelIcon2.setIcon(logo_product);
				labelIcon2.setText("SPFamily");
				labelIcon2.setVisible(true);
				uninstallApp2.setEnabled(true);
				uninstallApp2.setVisible(true);
				enableFirebase2.setEnabled(true);
				enableFirebase2.setVisible(true);
				saveLogsButton2.setEnabled(true);
				break;
			}
			case "com.smithmicro.att.securefamily":
			case "com.att.securefamilycompanion": {
				labelIcon2.setIcon(logo_att);
				labelIcon2.setText("SecureFamily");
				labelIcon2.setVisible(true);
				uninstallApp2.setEnabled(true);
				uninstallApp2.setVisible(true);
				enableFirebase2.setEnabled(true);
				enableFirebase2.setVisible(true);
				saveLogsButton2.setEnabled(true);
				break;
			}
			case "com.smithmicro.sprint.safeandfound.test":
			case "com.sprint.safefound": {
				labelIcon2.setIcon(logo_sprint);
				labelIcon2.setText("Safe&Found");
				labelIcon2.setVisible(true);
				uninstallApp2.setEnabled(true);
				uninstallApp2.setVisible(true);
				enableFirebase2.setEnabled(true);
				enableFirebase2.setVisible(true);
				saveLogsButton2.setEnabled(true);
				break;
			}
			case "com.smithmicro.orangespain.test": {
				labelIcon2.setIcon(logo_orange);
				labelIcon2.setText("TuYo");
				labelIcon2.setVisible(true);
				uninstallApp2.setEnabled(true);
				uninstallApp2.setVisible(true);
				enableFirebase2.setEnabled(true);
				enableFirebase2.setVisible(true);
				saveLogsButton2.setEnabled(true);
				break;
			}
			case "": {
				labelIcon2.setIcon(notInstalled);
				labelIcon2.setText("Not Installed");
				labelIcon2.setVisible(true);
				uninstallApp2.setVisible(true);
				enableFirebase2.setVisible(true);
				saveLogsButton2.setEnabled(false);
				break;
			}
		}
		saveLogsButton2.setVisible(true);
		wifiDebug2.setText("WiFi Debug");
		if (listOfDevices.get(i).endsWith(":5555")) {
			wifiDebug2.setText("Disable WiFi");
		}
		wifiDebug2.setVisible(true);
		enableFirebase2.setVisible(true);
		reboot2.setVisible(true);
		device2TextPane.setText(listOfDevices.get(i) + "\n" + utility.getDeviceManufacturer(listOfDevices.get(i)) + "\n"
				+ utility.getDeviceModel(listOfDevices.get(i)) + "\n" + "Android "
				+ utility.getDeviceOSVersion(listOfDevices.get(i)) + "\n" + ips.get(i));
		device2TextPane.setVisible(true);
		takeScreenshotButton2.setVisible(true);
		int j = 0;
		while (j < listOfDevices.size()) {
			if (utility.checkIfInstalled(listOfDevices.get(j))) {
				uninstallAllButton.setEnabled(true);
				break;
			}
			j++;
		}
	}

	private void setIconAndButtons1(int i) {
		radio1.setSelected(true);
		radio1.setVisible(true);
		String packageName1 = utility.getSafePathPackage(listOfDevices.get(i));
		switch (packageName1) {
			case "com.smithmicro.tmobile.familymode.test":
			case "com.tmobile.familycontrols": {
				labelIcon1.setIcon(logo_tmo);
				labelIcon1.setText("FamilyMode");
				labelIcon1.setVisible(true);
				uninstallApp1.setEnabled(true);
				uninstallApp1.setVisible(true);
				enableFirebase1.setEnabled(true);
				enableFirebase1.setVisible(true);
				saveLogsButton1.setEnabled(true);
				break;
			}
			case "com.smithmicro.safepath.family":
			case "com.smithmicro.safepath.family.child": {
				labelIcon1.setIcon(logo_product);
				labelIcon1.setText("SPFamily");
				labelIcon1.setVisible(true);
				uninstallApp1.setEnabled(true);
				uninstallApp1.setVisible(true);
				enableFirebase1.setEnabled(true);
				enableFirebase1.setVisible(true);
				saveLogsButton1.setEnabled(true);
				break;
			}
			case "com.smithmicro.att.securefamily":
			case "com.att.securefamilycompanion": {
				labelIcon1.setIcon(logo_att);
				labelIcon1.setText("SecureFamily");
				labelIcon1.setVisible(true);
				uninstallApp1.setEnabled(true);
				uninstallApp1.setVisible(true);
				enableFirebase1.setEnabled(true);
				enableFirebase1.setVisible(true);
				saveLogsButton1.setEnabled(true);
				break;
			}
			case "com.smithmicro.sprint.safeandfound.test":
			case "com.sprint.safefound": {
				labelIcon1.setIcon(logo_sprint);
				labelIcon1.setText("Safe&Found");
				labelIcon1.setVisible(true);
				uninstallApp1.setEnabled(true);
				uninstallApp1.setVisible(true);
				enableFirebase1.setEnabled(true);
				enableFirebase1.setVisible(true);
				saveLogsButton1.setEnabled(true);
				break;
			}
			case "com.smithmicro.orangespain.test": {
				labelIcon1.setIcon(logo_orange);
				labelIcon1.setText("TuYo");
				labelIcon1.setVisible(true);
				uninstallApp1.setEnabled(true);
				uninstallApp1.setVisible(true);
				enableFirebase1.setEnabled(true);
				enableFirebase1.setVisible(true);
				saveLogsButton1.setEnabled(true);
				break;
			}
			case "": {
				labelIcon1.setIcon(notInstalled);
				labelIcon1.setText("Not Installed");
				labelIcon1.setVisible(true);
				uninstallApp1.setVisible(true);
				enableFirebase1.setVisible(true);
				saveLogsButton1.setEnabled(false);
				break;
			}
		}
		saveLogsButton1.setVisible(true);
		wifiDebug1.setText("WiFi Debug");
		if (listOfDevices.get(i).endsWith(":5555")) {
			wifiDebug1.setText("Disable WiFi");
		}
		wifiDebug1.setVisible(true);
		enableFirebase1.setVisible(true);
		reboot1.setVisible(true);
		device1TextPane.setText(listOfDevices.get(i) + "\n" + utility.getDeviceManufacturer(listOfDevices.get(i)) + "\n"
				+ utility.getDeviceModel(listOfDevices.get(i)) + "\n" + "Android "
				+ utility.getDeviceOSVersion(listOfDevices.get(i)) + "\n" + ips.get(i));
		device1TextPane.setVisible(true);
		takeScreenshotButton1.setVisible(true);
		int j = 0;
		while (j < listOfDevices.size()) {
			if (utility.checkIfInstalled(listOfDevices.get(j))) {
				uninstallAllButton.setEnabled(true);
				break;
			}
			j++;
		}
	}
	class InstallButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			listOfDevices = utility.getConnectedDevices();
			progressBar.setString("Installing...");
			progressBar.setIndeterminate(true);
			progressBar.setBackground(new Color(238, 238, 238));
			installButton.setEnabled(false);

			if (radio1.isSelected() && listOfDevices.size() > 0) {
				radio1State = true;
				Task task1 = new Task("adb -s " + listOfDevices.get(0) + " install " + "\"" + file1.getAbsolutePath() + "\"");
				task1.addPropertyChangeListener(null);
				task1.execute();
			} else {
				radio1State = false;
			}

			if (radio2.isSelected() && listOfDevices.size() > 1) {
				radio2State = true;
				Task task2 = new Task("adb -s " + listOfDevices.get(1) + " install " + "\"" + file1.getAbsolutePath() + "\"");
				task2.addPropertyChangeListener(null);
				task2.execute();
			} else {
				radio2State = false;
			}
			if (radio3.isSelected() && listOfDevices.size() > 2) {
				radio3State = true;
				Task task3 = new Task("adb -s " + listOfDevices.get(2) + " install " + "\"" + file1.getAbsolutePath() + "\"");
				task3.addPropertyChangeListener(null);
				task3.execute();
			} else {
				radio3State = false;
			}
			if (radio4.isSelected() && listOfDevices.size() > 3) {
				radio4State = true;
				Task task4 = new Task("adb -s " + listOfDevices.get(3) + " install " + "\"" + file1.getAbsolutePath() + "\"");
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
			listOfDevices = utility.getConnectedDevices();
			progressBar.setString("Uninstalling...");
			progressBar.setIndeterminate(true);
			progressBar.setBackground(new Color(238, 238, 238));
			uninstallAllButton.setEnabled(false);
			int i = 0;
			Task[] task = new Task[4];
			while (i < listOfDevices.size()) {
				task[i] = new Task("adb -s " + listOfDevices.get(i) + " shell pm uninstall "
						+ utility.getSafePathPackage(listOfDevices.get(i)));
				task[i].addPropertyChangeListener(null);
				task[i].execute();
				i++;
			}
		}
	}
	class FileButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			File default_location = new File("C:/AdbToolkit/location.ser");
			if (default_location.exists()) {
				try {
					ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("C:/AdbToolkit/location.ser"));
					default_location = (File) objectInputStream.readObject();
				} catch (IOException | ClassNotFoundException ex) {
					throw new RuntimeException(ex);
				}
			}
			JFileChooser fileChooser = new JFileChooser(default_location.getAbsolutePath());
			int response = fileChooser.showOpenDialog(null);
			if (response == JFileChooser.APPROVE_OPTION) {
				file1 = new File(fileChooser.getSelectedFile().getAbsolutePath());
				fileTextField.setText(file1.getName());
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
				file1 = new File(fileChooser.getSelectedFile().getAbsolutePath());
				listOfDevices = utility.getConnectedDevices();
				String appFlavour = utility.getSafePathPackage(listOfDevices.get(0));
				utility.saveLogs(listOfDevices.get(0), appFlavour, file1.getAbsolutePath());
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
				file1 = new File(fileChooser.getSelectedFile().getAbsolutePath());
				listOfDevices = utility.getConnectedDevices();
				String appFlavour = utility.getSafePathPackage(listOfDevices.get(1));
				utility.saveLogs(listOfDevices.get(1), appFlavour, file1.getAbsolutePath());
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
				file1 = new File(fileChooser.getSelectedFile().getAbsolutePath());
				listOfDevices = utility.getConnectedDevices();
				String appFlavour = utility.getSafePathPackage(listOfDevices.get(2));
				utility.saveLogs(listOfDevices.get(2), appFlavour, file1.getAbsolutePath());
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
				file1 = new File(fileChooser.getSelectedFile().getAbsolutePath());
				listOfDevices = utility.getConnectedDevices();
				String appFlavour = utility.getSafePathPackage(listOfDevices.get(3));
				utility.saveLogs(listOfDevices.get(3), appFlavour, file1.getAbsolutePath());
				JOptionPane.showMessageDialog(null, "Safe Path logs saved!", "Safe Path Logs",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
	class TakeScreenshotButton1Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			listOfDevices = utility.getConnectedDevices();
			String output = utility.takeScreenshot(listOfDevices.get(0), "sdcard/", "screenshot.png");
			File device1 = new File("C:/AdbToolkit/Screenshots/Device1");
			if (!device1.exists()) {
				device1.mkdirs();
			}
			utility.pullFile(listOfDevices.get(0), output, "C:/AdbToolkit/Screenshots/Device1");
			JOptionPane.showMessageDialog(null,
					"Device1 screenshot captured!" + "\n" + "Location: C:/AdbToolkit/Screenshots/Device1",
					"Screenshot Capture", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	class TakeScreenshotButton2Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			listOfDevices = utility.getConnectedDevices();
			String output = utility.takeScreenshot(listOfDevices.get(1), "sdcard/", "screenshot.png");
			File device2 = new File("C:/AdbToolkit/Screenshots/Device2");
			if (!device2.exists()) {
				device2.mkdirs();
			}
			utility.pullFile(listOfDevices.get(1), output, "C:/AdbToolkit/Screenshots/Device2");
			JOptionPane.showMessageDialog(null,
					"Device2 screenshot captured!" + "\n" + "Location: C:/AdbToolkit/Screenshots/Device2",
					"Screenshot Capture", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	class TakeScreenshotButton3Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			listOfDevices = utility.getConnectedDevices();
			String output = utility.takeScreenshot(listOfDevices.get(2), "sdcard/", "screenshot.png");
			File device3 = new File("C:/AdbToolkit/Screenshots/Device3");
			if (!device3.exists()) {
				device3.mkdirs();
			}
			utility.pullFile(listOfDevices.get(2), output, "C:/AdbToolkit/Screenshots/Device3");
			JOptionPane.showMessageDialog(null,
					"Device3 screenshot captured!" + "\n" + "Location: C:/AdbToolkit_Screenshots/Device3",
					"Screenshot Capture", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	class TakeScreenshotButton4Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			listOfDevices = utility.getConnectedDevices();
			String output = utility.takeScreenshot(listOfDevices.get(3), "sdcard/", "screenshot.png");
			File device4 = new File("C:/AdbToolkit/Screenshots/Device4");
			if (!device4.exists()) {
				device4.mkdirs();
			}
			utility.pullFile(listOfDevices.get(3), output, "C:/AdbToolkit/Screenshots/Device4");
			JOptionPane.showMessageDialog(null,
					"Device4 screenshot captured!" + "\n" + "Location: C:/AdbToolkit/Screenshots/Device4",
					"Screenshot Capture", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	class WifiDebug1Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!listOfDevices.get(0).endsWith(":5555")) {
				utility.startWifiDebugging(listOfDevices.get(0), ips.get(0));
				JOptionPane.showMessageDialog(
						null,
						"Debugging over WiFi is enabled!\n" +
								"If prompted on the device, allow wireless debugging on specific wifi network.\n" +
								"You may disconnect USB cable from this device.",
						"Enable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				wifiDebug4.setText("Disable WiFi");
				devicesButton.doClick(200);
			} else {
				utility.stopWifiDebugging(listOfDevices.get(0), ips.get(0));
				JOptionPane.showMessageDialog(null, "Debugging over WiFi is disabled!", "Disable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				devicesButton.doClick(100);
			}
		}
	}
	class WifiDebug2Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!listOfDevices.get(1).endsWith(":5555")) {
				utility.startWifiDebugging(listOfDevices.get(1), ips.get(1));
				JOptionPane.showMessageDialog(
						null,
						"Debugging over WiFi is enabled!\n" +
								"If prompted on the device, allow wireless debugging on specific wifi network.\n" +
								"You may disconnect USB cable from this device.",
						"Enable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				wifiDebug4.setText("Disable WiFi");
				devicesButton.doClick(200);
			} else {
				utility.stopWifiDebugging(listOfDevices.get(1), ips.get(1));
				JOptionPane.showMessageDialog(null, "Debugging over WiFi is disabled!", "Disable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				devicesButton.doClick(100);
			}
		}
	}
	class WifiDebug3Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!listOfDevices.get(2).endsWith(":5555")) {
				utility.startWifiDebugging(listOfDevices.get(2), ips.get(2));
				JOptionPane.showMessageDialog(
						null,
						"Debugging over WiFi is enabled!\n" +
								"If prompted on the device, allow wireless debugging on specific wifi network.\n" +
								"You may disconnect USB cable from this device.",
						"Enable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				wifiDebug4.setText("Disable WiFi");
				devicesButton.doClick(200);
			} else {
				utility.stopWifiDebugging(listOfDevices.get(2), ips.get(2));
				JOptionPane.showMessageDialog(null, "Debugging over WiFi is disabled!", "Disable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				devicesButton.doClick(100);
			}
		}
	}
	class WifiDebug4Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!listOfDevices.get(3).endsWith(":5555")) {
				utility.startWifiDebugging(listOfDevices.get(3), ips.get(3));
				JOptionPane.showMessageDialog(
						null,
						"Debugging over WiFi is enabled!\n" +
								"If prompted on the device, allow wireless debugging on specific wifi network.\n" +
								"You may disconnect USB cable from this device.",
						"Enable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				wifiDebug4.setText("Disable WiFi");
				devicesButton.doClick(200);
			} else {
				utility.stopWifiDebugging(listOfDevices.get(3), ips.get(3));
				JOptionPane.showMessageDialog(null, "Debugging over WiFi is disabled!", "Disable WiFi Debugging.",
						JOptionPane.INFORMATION_MESSAGE);
				devicesButton.doClick(100);
			}
		}
	}
	class EnableFirebase1Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			listOfDevices = utility.getConnectedDevices();
			utility.enableAnalyticsDebug(listOfDevices.get(0), utility.getSafePathPackage(listOfDevices.get(0)));
			if (utility.checkIfInstalled(listOfDevices.get(0))) {
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
			listOfDevices = utility.getConnectedDevices();
			utility.enableAnalyticsDebug(listOfDevices.get(1), utility.getSafePathPackage(listOfDevices.get(1)));
			if (utility.checkIfInstalled(listOfDevices.get(1))) {
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
			listOfDevices = utility.getConnectedDevices();
			utility.enableAnalyticsDebug(listOfDevices.get(2), utility.getSafePathPackage(listOfDevices.get(2)));
			if (utility.checkIfInstalled(listOfDevices.get(2))) {
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
			listOfDevices = utility.getConnectedDevices();
			utility.enableAnalyticsDebug(listOfDevices.get(3), utility.getSafePathPackage(listOfDevices.get(3)));
			if (utility.checkIfInstalled(listOfDevices.get(3))) {
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
			utility.reboot(listOfDevices.get(0));
		}
	}
	class Reboot2Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			utility.reboot(listOfDevices.get(1));
		}
	}
	class Reboot3Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			utility.reboot(listOfDevices.get(2));
		}
	}
	class Reboot4Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			utility.reboot(listOfDevices.get(3));
		}
	}
	class UninstallApp1Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			utility.uninstallApp(listOfDevices.get(0), utility.getSafePathPackage(listOfDevices.get(0)));
			JOptionPane.showMessageDialog(null, "App is uninstalled!", "Enable WiFi Debugging.",
					JOptionPane.INFORMATION_MESSAGE);
			saveLogsButton1.setEnabled(false);
			devicesButton.doClick();
		}
	}
	class UninstallApp2Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			utility.uninstallApp(listOfDevices.get(1), utility.getSafePathPackage(listOfDevices.get(1)));
			JOptionPane.showMessageDialog(null, "App is uninstalled!", "Enable WiFi Debugging.",
					JOptionPane.INFORMATION_MESSAGE);
			saveLogsButton2.setEnabled(false);
			devicesButton.doClick();
		}
	}
	class UninstallApp3Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			utility.uninstallApp(listOfDevices.get(2), utility.getSafePathPackage(listOfDevices.get(2)));
			JOptionPane.showMessageDialog(null, "App is uninstalled!", "Enable WiFi Debugging.",
					JOptionPane.INFORMATION_MESSAGE);
			saveLogsButton3.setEnabled(false);
			devicesButton.doClick();
		}
	}
	class UninstallApp4Listener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			utility.uninstallApp(listOfDevices.get(3), utility.getSafePathPackage(listOfDevices.get(3)));
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
			utility.runCommand(command);
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