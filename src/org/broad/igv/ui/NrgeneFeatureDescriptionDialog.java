package org.broad.igv.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.ui.util.FileDialogUtils;

@SuppressWarnings("serial")
public class NrgeneFeatureDescriptionDialog extends JDialog {

    private static Logger log = Logger.getLogger(NrgeneFeatureDescriptionDialog.class);

	
	@SuppressWarnings("unused")
	private Frame				parentFrame;
	private JScrollPane			scrollPanel;
	private JTextArea			area;
	
	public NrgeneFeatureDescriptionDialog(Frame parentFrame, String text)
	{
		this(parentFrame, text, false);
	}
	
	public NrgeneFeatureDescriptionDialog(Frame parentFrame, String text, boolean saveTo)
	{
		// super & setup
		super(parentFrame);
		this.parentFrame = parentFrame;
		
		// dialog behavior
		setTitle("Feature Description");
		setSize(400, 450);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		// extract url from text if exists
		final String	urlText = extractUrl(text);
		
		// create contents components
		JPanel			topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		getContentPane().add(topPanel);
		
		// populate
		area = new JTextArea();
		area.setTabSize(4);
		area.setText(text);
		scrollPanel = new JScrollPane();
		scrollPanel.getViewport().add(area);
		area.setMargin(new Insets(5, 5, 5, 5));
		topPanel.add(scrollPanel, BorderLayout.CENTER);		
		
		// buttons
		JPanel			toolbar = new JPanel();
		toolbar.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		if ( urlText != null )
		{
			JButton			openUrl = new JButton("Open URL");
			toolbar.add(openUrl);
			openUrl.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent arg0) {
					
					if ( Desktop.isDesktopSupported() )
					{
						try {
							Desktop.getDesktop().browse(new URI(urlText));
						} catch (Exception e) {
							log.error("failed to browse into: " + urlText, e);
						}
					}
				}
			});			
		}
		
		if ( saveTo )
		{
			JButton			save = new JButton("Save ...");
			toolbar.add(save);
			save.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent arg0) {
					
			        File 	textFile = null;
			        IGV		mainFrame = IGV.getInstance();
			        String 	currenttextFilePath = mainFrame.getSession().getPath();

			        String initFile = "igv_stats.txt";
			        File initFolder = currenttextFilePath == null 
			        		? PreferenceManager.getInstance().getLastSessionDirectory() 
			        		: (new File(currenttextFilePath).getParentFile());
			        
			        textFile = FileDialogUtils.chooseFile("Save File",
			                initFolder,
			                new File(initFile),
			                FileDialogUtils.SAVE);


			        if (textFile == null) {
			            mainFrame.resetStatusMessage();
			            return;
			        }


			        String filePath = textFile.getAbsolutePath();
			        if (!filePath.toLowerCase().endsWith(".txt")) {
			            textFile = new File(filePath + ".txt");
			        }

			        mainFrame.setStatusBarMessage("Saving to " + textFile.getAbsolutePath());


			        final File sf = textFile;
			        WaitCursorManager.CursorToken token = WaitCursorManager.showWaitCursor();
			        try 
			        {
			        	String		text = area.getText();
			        	
			        	PrintWriter	pw = new PrintWriter(sf);
			        	pw.append(text);
			        	pw.close();

			        } catch (Exception e2) {
			            JOptionPane.showMessageDialog(mainFrame.getMainFrame(), "There was an error writing to " + sf.getName() + "(" + e2.getMessage() + ")");
			            log.error("Failed to save file!", e2);
			        } finally {
			            WaitCursorManager.removeWaitCursor(token);
			            mainFrame.resetStatusMessage();
			        }

				}
			});			
		}

		JButton			copy = new JButton("Copy");
		toolbar.add(copy);
		copy.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				
				String			text = area.getSelectedText();
				if ( text == null || text.length() == 0 )
					text = area.getText();
				
				StringSelection stringSelection = new StringSelection(text);
			    Clipboard 		clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			    clipboard.setContents(stringSelection, null);
			}
		});

		JButton			ok = new JButton("OK");
		toolbar.add(ok);
		ok.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				setVisible(false);
			}
		});
				

		topPanel.add(toolbar, BorderLayout.SOUTH);
		
		// scroll to top
		area.setCaretPosition(0);
	}
	
	public void selectText(int start, int end)
	{
		area.setSelectionEnd(end);
		area.setSelectionStart(start);
		area.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
	}
	
	public void selectAll()
	{
		selectText(0, area.getText().length());
	}

	static String extractUrl(String text) 
	{
		Pattern		pattern = Pattern.compile("\n[Uu][Rr][Ll] ?[:=] ?(.+)\n");
		Matcher		matcher = pattern.matcher(text);
		if ( matcher.find() )
			return matcher.group(1);
		else
			return null;
	}
	
	static public void main(String[] args)
	{
		String[]		testDataArray = 
		{
			"",
			"\nURL: check\n",
			"\nURL:check\n",
			"xxx\nURL: check\nxxx\n",
			"xxx\nURL = check\nxxx\n",
			"xxx\nx-URL: check\nxxx\n"
		};
		
		for ( String testData : testDataArray )
		{
			String		urlText = extractUrl(testData);
			
			System.out.println(testData.replace("\n", "\\n") + " -> " + urlText);
		}
	}
}
