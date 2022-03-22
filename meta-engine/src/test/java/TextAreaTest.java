import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.hoddmimes.te.common.GridBagPanel;
import com.hoddmimes.te.management.gui.mgmt.Management;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;

public class TextAreaTest extends JFrame
{
	JsonObject jConfiguration;
	TextArea mTextArea;

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		TextAreaTest tat = new TextAreaTest();
		tat.loadJson();
		tat.initPanel();
		tat.pack();
		tat.setVisible( true );
	}

	private void loadJson() {
		File tFile = new File("./configuration/TeConfiguration.json");
		try {
			jConfiguration = JsonParser.parseReader( new JsonReader( new FileReader( tFile) )).getAsJsonObject();

		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
	private void initPanel() {
		GridBagPanel tRootPanel = new GridBagPanel( GridBagConstraints.CENTER );
		tRootPanel.insets( new Insets(10,10,10,10));

		JLabel tJLabel = new JLabel("TEXT AREA TEST");
		tJLabel.setFont( new Font("Arial", Font.BOLD, 28));
		tRootPanel.add( tJLabel );

		mTextArea = new TextArea("Hello World");
		mTextArea.setFont( new Font("Arial", Font.PLAIN, 10));
		tRootPanel.incy().add( mTextArea );


		JButton tSetJsonButton =  new JButton("Set JSON Data");
		tSetJsonButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setJsonData();
			}
		});

		tRootPanel.bottom(20).incy().add( tSetJsonButton);
		this.setContentPane( tRootPanel );
	}

	private void setJsonData() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String tData = gson.toJson(jConfiguration);
		mTextArea.setText( tData );
		mTextArea.validate();
		mTextArea.repaint();
	}

}
