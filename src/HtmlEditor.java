import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.html.*;


public class HtmlEditor extends javax.swing.JFrame {
	
	private static final long serialVersionUID = 1L;

	public static final String APP_NAME = "WebDev For Everyone";
   
    protected StyleSheet m_context;
	protected MutableHTMLDocument m_doc;	
	protected CustomHTMLEditorKit m_kit;
	protected JToolBar m_toolBar;
        
    protected SimpleFilter m_htmlFilter;
	protected JFileChooser m_chooser;
	protected File  m_currentFile;

	protected boolean m_textChanged = false;
        
    protected String m_fontName = "";
	protected int m_fontSize = 0;
	protected boolean m_skipUpdate;

	protected int m_xStart = -1;
	protected int m_xFinish = -1;
	
	
	class MutableHTMLDocument extends HTMLDocument {

		public MutableHTMLDocument(StyleSheet styles) {
			super(styles);
		}

		public Element getElementByTag(HTML.Tag tag) {
			Element root = getDefaultRootElement();
			return getElementByTag(root, tag);
		}

		public Element getElementByTag(Element parent, HTML.Tag tag) {
			if (parent == null || tag == null)
				return null;
			for (int k=0; k<parent.getElementCount(); k++) {
				Element child = parent.getElement(k);
				if (child.getAttributes().getAttribute(
						StyleConstants.NameAttribute).equals(tag))
					return child;
				Element e = getElementByTag(child, tag);
				if (e != null)
					return e;
			}
			return null;
		}

		public String getTitle() {
			return (String)getProperty(Document.TitleProperty);
		}

		// This will work only if <title> element was
		// previously created. Looks like a bug in HTML package.
		public void setTitle(String title) {
			Dictionary<Object, Object> di = getDocumentProperties();
			di.put(Document.TitleProperty, title);
			setDocumentProperties(di);
		}

		public void addAttributes(Element e, AttributeSet attributes) {
			if (e == null || attributes == null)
				return;
			try {
				writeLock();
				MutableAttributeSet mattr =
					(MutableAttributeSet)e.getAttributes();
				mattr.addAttributes(attributes);
				fireChangedUpdate(new AbstractDocument.DefaultDocumentEvent(0, getLength(),
					DocumentEvent.EventType.CHANGE));
			}
			finally {
				writeUnlock();
			}
		}
	}
	
	class CustomHTMLEditorKit extends HTMLEditorKit {

		public Document createDocument() {
			StyleSheet styles = getStyleSheet();
			StyleSheet ss = new StyleSheet();

			ss.addStyleSheet(styles);

			MutableHTMLDocument doc = new MutableHTMLDocument(ss);
			doc.setParser(getParser());
			doc.setAsynchronousLoadPriority(4);
			doc.setTokenThreshold(100);
			return doc;
		}

	}
	
	class SimpleFilter extends javax.swing.filechooser.FileFilter
	   {
	   	private String m_description = null;
	   	private String m_extension = null;

	   	public SimpleFilter(String extension, String description) {
	   		m_description = description;
	   		m_extension = "."+extension.toLowerCase();
	   	}

	   	public String getDescription() {
	   		return m_description;
	   	}

	   	public boolean accept(File f) {
	   		if (f == null)
	   			return false;
	   		if (f.isDirectory())
	   			return true;
	   		return f.getName().toLowerCase().endsWith(m_extension);
	   	}
	           
	           
	   }
	
	class UpdateListener implements DocumentListener {
		
		public void insertUpdate(DocumentEvent e) {
			m_textChanged = true;
		}

		public void removeUpdate(DocumentEvent e) {
			m_textChanged = true;
		}

		public void changedUpdate(DocumentEvent e) {
			m_textChanged = true;
		}
	}
	
	class HtmlSourceDlg extends JDialog {
		protected boolean m_succeeded = false;

		protected JTextArea m_sourceTxt;

		public HtmlSourceDlg(JFrame parent, String source) {
		super(parent, "HTML Source", true);

		JPanel pp = new JPanel(new BorderLayout());
		pp.setBorder(new EmptyBorder(10, 10, 5, 10));

		m_sourceTxt = new JTextArea(source, 20, 60);
		m_sourceTxt.setFont(new Font("Courier", Font.PLAIN, 12));
		JScrollPane sp = new JScrollPane(m_sourceTxt);
		pp.add(sp, BorderLayout.CENTER);

		JPanel p = new JPanel(new FlowLayout());
		JPanel p1 = new JPanel(new GridLayout(1, 2, 10, 0));
		JButton bt = new JButton("Save");
		ActionListener lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_succeeded = true;
				dispose();
			}
		};
		bt.addActionListener(lst);
		p1.add(bt);

		bt = new JButton("Cancel");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		bt.addActionListener(lst);
		p1.add(bt);
		p.add(p1);
		pp.add(p, BorderLayout.SOUTH);

		getContentPane().add(pp, BorderLayout.CENTER);
		pack();
		setResizable(true);
		setLocationRelativeTo(parent);
	}
	
	public boolean succeeded() {
		return m_succeeded;
	}

	public String getSource() {
		return m_sourceTxt.getText();
	}
}
	
	public static class Utils{
		// Copied from javax.swing.text.html.CSS class
		// Why it is not public? Sort of paranoya...
	       public static String colorToHex(Color color) {
		  String colorstr = new String("#");

			// Red
			String str = Integer.toHexString(color.getRed());
			if (str.length() > 2)
				str = str.substring(0, 2);
			else if (str.length() < 2)
				colorstr += "0" + str;
			else
				colorstr += str;

			// Green
			str = Integer.toHexString(color.getGreen());
			if (str.length() > 2)
				str = str.substring(0, 2);
			else if (str.length() < 2)
				colorstr += "0" + str;
			else
				colorstr += str;

			// Blue
			str = Integer.toHexString(color.getBlue());
			if (str.length() > 2)
				str = str.substring(0, 2);
			else if (str.length() < 2)
				colorstr += "0" + str;
			else
				colorstr += str;

			return colorstr;
		}
	}
	
	class DocumentPropsDlg extends JDialog {
		protected boolean m_succeeded = false;
		protected MutableHTMLDocument m_doc;

		protected Color m_backgroundColor;
		protected Color m_textColor;
		protected Color m_linkColor;
		protected Color m_viewedColor;

		protected JTextField m_titleTxt;
		protected JTextPane m_previewPane;

		public DocumentPropsDlg(JFrame parent, MutableHTMLDocument doc) {
			super(parent, "Page Properties", true);
			m_doc = doc;

			Element body = m_doc.getElementByTag(HTML.Tag.BODY);
			if (body != null) {
				AttributeSet attr = body.getAttributes();
				StyleSheet syleSheet = m_doc.getStyleSheet();
				Object obj = attr.getAttribute(HTML.Attribute.BGCOLOR);
				if (obj != null)
					m_backgroundColor = syleSheet.stringToColor((String)obj);
				obj = attr.getAttribute(HTML.Attribute.TEXT);
				if (obj != null)
					m_textColor = syleSheet.stringToColor((String)obj);
				obj = attr.getAttribute(HTML.Attribute.LINK);
				if (obj != null)
					m_linkColor = syleSheet.stringToColor((String)obj);
				obj = attr.getAttribute(HTML.Attribute.VLINK);
				if (obj != null)
					m_viewedColor = syleSheet.stringToColor((String)obj);
			}

			ActionListener lst;
			JButton bt;

			JPanel pp = new JPanel();
			pp.setBorder(new EmptyBorder(10, 10, 5, 10));

			pp.add(new JLabel("Page title:"));
			m_titleTxt = new JTextField(m_doc.getTitle(), 24);
			pp.add(m_titleTxt);

			JPanel pa = new JPanel(new BorderLayout(5, 5));
			Border ba = new TitledBorder(new EtchedBorder(
				EtchedBorder.RAISED), "Appearance");
			pa.setBorder(new CompoundBorder(ba, new EmptyBorder(0, 5, 5, 5)));

			JPanel pb = new JPanel(new GridLayout(4, 1, 5, 5));
			bt = new JButton("Background");
			bt.setMnemonic('b');
			lst = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					m_backgroundColor = JColorChooser.showDialog(DocumentPropsDlg.this,
						"Document Background", m_backgroundColor);
					showColors();
				}
			};
			bt.addActionListener(lst);
			pb.add(bt);

			bt = new JButton("Text");
			bt.setMnemonic('t');
			lst = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					m_textColor = JColorChooser.showDialog(DocumentPropsDlg.this,
						"Text Color", m_textColor);
					showColors();
				}
			};
			bt.addActionListener(lst);
			pb.add(bt);

			bt = new JButton("Link");
			bt.setMnemonic('l');
			lst = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					m_linkColor = JColorChooser.showDialog(DocumentPropsDlg.this,
						"Links Color", m_linkColor);
					showColors();
				}
			};
			bt.addActionListener(lst);
			pb.add(bt);

			bt = new JButton("Viewed");
			bt.setMnemonic('v');
			lst = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					m_viewedColor = JColorChooser.showDialog(DocumentPropsDlg.this,
						"Viewed Links Color", m_viewedColor);
					showColors();
				}
			};
			bt.addActionListener(lst);
			pb.add(bt);
			pa.add(pb, BorderLayout.WEST);

			m_previewPane = new JTextPane();
			m_previewPane.setBackground(Color.white);
			m_previewPane.setEditable(false);
			m_previewPane.setBorder(new CompoundBorder(
				new BevelBorder(BevelBorder.LOWERED),
				new EmptyBorder(10, 10, 10, 10)));
			showColors();
			pa.add(m_previewPane, BorderLayout.CENTER);

			pp.add(pa);

			bt = new JButton("Save");
			lst = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveData();
					dispose();
				}
			};
			bt.addActionListener(lst);
			pp.add(bt);

			bt = new JButton("Cancel");
			lst = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			};
			bt.addActionListener(lst);
			pp.add(bt);

			getContentPane().add(pp, BorderLayout.CENTER);
			pack();
			setResizable(false);
			setLocationRelativeTo(parent);
		}

		public boolean succeeded() {
			return m_succeeded;
		}


	protected void saveData() {
			m_doc.setTitle(m_titleTxt.getText());

			Element body = m_doc.getElementByTag(HTML.Tag.BODY);
			MutableAttributeSet attr = new SimpleAttributeSet();
			if (m_backgroundColor != null)
				attr.addAttribute(HTML.Attribute.BGCOLOR,
					Utils.colorToHex(m_backgroundColor));
			if (m_textColor != null)
				attr.addAttribute(HTML.Attribute.TEXT,
					Utils.colorToHex(m_textColor));
			if (m_linkColor != null)
				attr.addAttribute(HTML.Attribute.LINK,
					Utils.colorToHex(m_linkColor));
			if (m_viewedColor != null)
				attr.addAttribute(HTML.Attribute.VLINK,
					Utils.colorToHex(m_viewedColor));
			m_doc.addAttributes(body, attr);

			m_succeeded = true;
		}

		protected void showColors() {
			DefaultStyledDocument doc = new DefaultStyledDocument();

			SimpleAttributeSet attr = new SimpleAttributeSet();
			StyleConstants.setFontFamily(attr, "Arial");
			StyleConstants.setFontSize(attr, 14);
			if (m_backgroundColor != null) {
				StyleConstants.setBackground(attr, m_backgroundColor);
				m_previewPane.setBackground(m_backgroundColor);
			}

			try {
				StyleConstants.setForeground(attr, m_textColor!=null ?
					m_textColor : Color.black);
				doc.insertString(doc.getLength(), "Plain text preview\n\n", attr);

				StyleConstants.setForeground(attr, m_linkColor!=null ?
					m_linkColor : Color.blue);
				StyleConstants.setUnderline(attr, true);
				doc.insertString(doc.getLength(), "Link preview\n\n", attr);

				StyleConstants.setForeground(attr, m_viewedColor!=null ?
					m_viewedColor : Color.magenta);
				StyleConstants.setUnderline(attr, true);
				doc.insertString(doc.getLength(), "Viewed link preview\n", attr);
			}
			catch (BadLocationException be) {
				be.printStackTrace();
			}
			m_previewPane.setDocument(doc);
		}
	}
	
	private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JMenu mFormat;
    private javax.swing.JToggleButton m_bBold;
    private javax.swing.JToggleButton m_bItalic;
    private javax.swing.JToggleButton m_bUL;
    private javax.swing.JComboBox<String> m_cbFonts;
    private javax.swing.JComboBox<String> m_cbSizes;
    private javax.swing.JTextPane m_editor;
    private javax.swing.JMenuBar menubar;
    
	
	private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        m_editor = new javax.swing.JTextPane();
        jToolBar1 = new javax.swing.JToolBar();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        m_cbSizes = new javax.swing.JComboBox<>();
        m_cbFonts = new javax.swing.JComboBox<>();
        m_bBold = new javax.swing.JToggleButton();
        m_bItalic = new javax.swing.JToggleButton();
        m_bUL = new javax.swing.JToggleButton();
        menubar = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        mFormat = new javax.swing.JMenu();
        jMenuItem8 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        jButton1.setText("View Source!");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        getContentPane().add(jButton1);
        jButton1.setBounds(670, 10, 110, 30);

        jScrollPane1.setViewportView(m_editor);

        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(10, 50, 940, 550);

        jToolBar1.setRollover(true);

        jButton2.setText("New");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               jButton2ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton2);

        jButton3.setText("Open");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               jButton3ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton3);

        jButton4.setText("Save");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton4);

        m_cbSizes.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "8", "9", "10", "12", "14", "15", "18", "20", "22", "24", "26", "28", "30", "32", "34", "40", "60", "72" }));
        m_cbSizes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               m_cbSizesActionPerformed(evt);
            }
        });
        jToolBar1.add(m_cbSizes);

        m_cbFonts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               m_cbFontsActionPerformed(evt);
            }
        });
        jToolBar1.add(m_cbFonts);

        m_bBold.setText("Bold");
        m_bBold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               m_bBoldActionPerformed(evt);
            }
        });
        jToolBar1.add(m_bBold);

        m_bItalic.setText("Italics");
        m_bItalic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               m_bItalicActionPerformed(evt);
            }
        });
        jToolBar1.add(m_bItalic);

        m_bUL.setText("Underline");
        m_bUL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               m_bULActionPerformed(evt);
            }
        });
        jToolBar1.add(m_bUL);

        getContentPane().add(jToolBar1);
        jToolBar1.setBounds(0, 0, 540, 40);

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setText("New");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem2.setText("Open");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem3.setText("Save");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem4.setText("Save As..");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem4);

        jMenuItem5.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem5.setText("Exit");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem5);

        menubar.add(jMenu1);

        jMenu2.setText("Insert");

        jMenuItem6.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem6.setText("Image");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem6);

        jMenuItem7.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem7.setText("Hyperlink");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
              jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem7);

        menubar.add(jMenu2);

        mFormat.setText("Format");

        jMenuItem8.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem8.setText("Page Properties");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        mFormat.add(jMenuItem8);

        menubar.add(mFormat);

        setJMenuBar(menubar);

        pack();
    }
	
	public void showError(Exception ex, String message) {
		ex.printStackTrace();
		JOptionPane.showMessageDialog(this,
			message, APP_NAME,
			JOptionPane.WARNING_MESSAGE);
	}
	
	protected void showAttributes(int p) {
		m_skipUpdate = true;
		AttributeSet attr = m_doc.getCharacterElement(p).
			getAttributes();
		String name = StyleConstants.getFontFamily(attr);
		if (!m_fontName.equals(name)) {
			m_fontName = name;
			m_cbFonts.setSelectedItem(name);
		}
		int size = StyleConstants.getFontSize(attr);
		if (m_fontSize != size) {
			m_fontSize = size;
			m_cbSizes.setSelectedItem(Integer.toString(m_fontSize));
		}
		boolean bold = StyleConstants.isBold(attr);
		if (bold != m_bBold.isSelected())
			m_bBold.setSelected(bold);
		boolean italic = StyleConstants.isItalic(attr);
		if (italic != m_bItalic.isSelected())
			m_bItalic.setSelected(italic);
                boolean underline = StyleConstants.isUnderline(attr);
		if (underline != m_bUL.isSelected())
			m_bUL.setSelected(underline);
		m_skipUpdate = false;
	}
		
	protected boolean promptToSave() {
		if (!m_textChanged)
			return true;
		int result = JOptionPane.showConfirmDialog(this,
			"Save changes to "+getDocumentName()+"?",
			APP_NAME, JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.INFORMATION_MESSAGE);
		switch (result) {
		case JOptionPane.YES_OPTION:
			if (!saveFile(false))
				return false;
			return true;
		case JOptionPane.NO_OPTION:
			return true;
		case JOptionPane.CANCEL_OPTION:
			return false;
		}
		return true;
	}
	
	
	protected String getDocumentName() {
    	String title = m_doc.getTitle();	
	if (title != null && title.length() > 0)
		return title;
	return m_currentFile==null ? "Untitled" :
		m_currentFile.getName();
   }
	
	protected void newDocument() {
		m_doc = (MutableHTMLDocument)m_kit.createDocument();
		m_context = m_doc.getStyleSheet();

		m_editor.setDocument(m_doc);
		m_currentFile = null;
		setTitle(APP_NAME+" ["+getDocumentName()+"]");

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
                               showAttributes(0);
				m_editor.scrollRectToVisible(new Rectangle(0,0,1,1));
				m_doc.addDocumentListener(new UpdateListener());
				m_textChanged = false;
			}
		});
	}
	
	protected boolean saveFile(boolean saveAs) {
		if (!saveAs && !m_textChanged)
			return true;
		if (saveAs || m_currentFile == null) {
			if (m_chooser.showSaveDialog(HtmlEditor.this) !=
				JFileChooser.APPROVE_OPTION)
				return false;
			File f = m_chooser.getSelectedFile();
			if (f == null)
				return false;
			m_currentFile = f;
			setTitle(APP_NAME+" ["+getDocumentName()+"]");
		}

		HtmlEditor.this.setCursor(
			Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			OutputStream out = new FileOutputStream(m_currentFile);
			m_kit.write(out, m_doc, 0, m_doc.getLength());
			out.close();
			m_textChanged = false;
		}
		catch (Exception ex) {
			showError(ex, "Error saving file "+m_currentFile);
		}
		HtmlEditor.this.setCursor(Cursor.getPredefinedCursor(
			Cursor.DEFAULT_CURSOR));
		return true;
	}

	
	 public void documentChanged() {
			m_editor.setDocument(new HTMLDocument());
			m_editor.setDocument(m_doc);	// This alone will not work, since PropertyChange will not be fired
			m_editor.revalidate();
			m_editor.repaint();
			setTitle(APP_NAME+" ["+getDocumentName()+"]");
			m_textChanged = true;
		}  
	 
	 protected void openDocument() {
			if (m_chooser.showOpenDialog(HtmlEditor.this) !=
				JFileChooser.APPROVE_OPTION)
				return;
			File f = m_chooser.getSelectedFile();
			if (f == null || !f.isFile())
				return;
			m_currentFile = f;
			setTitle(APP_NAME+" ["+getDocumentName()+"]");

			HtmlEditor.this.setCursor(
				Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			try {
				InputStream in = new FileInputStream(m_currentFile);
				m_doc = (MutableHTMLDocument)m_kit.createDocument();
				m_kit.read(in, m_doc, 0);
				m_context = m_doc.getStyleSheet();
				m_editor.setDocument(m_doc);
				in.close();
			}
			catch (Exception ex) {
				showError(ex, "Error reading file "+m_currentFile);
			}
			HtmlEditor.this.setCursor(Cursor.getPredefinedCursor(
				Cursor.DEFAULT_CURSOR));

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					m_editor.setCaretPosition(1);
	                                showAttributes(1);
					m_editor.scrollRectToVisible(new Rectangle(0,0,1,1));
					m_doc.addDocumentListener(new UpdateListener());
					m_textChanged = false;
				}
			});
		}
	 
	 protected void setAttributeSet(AttributeSet attr) {
			if (m_skipUpdate)
				return;
			int xStart = m_editor.getSelectionStart();
			int xFinish = m_editor.getSelectionEnd();
			if (!m_editor.hasFocus()) {
				xStart = m_xStart;
				xFinish = m_xFinish;
			}
			if (xStart != xFinish) {
				m_doc.setCharacterAttributes(xStart, xFinish - xStart,
					attr, false);
			}
	                
			
			else {
				MutableAttributeSet inputAttributes =
					m_kit.getInputAttributes();
				inputAttributes.addAttributes(attr);
			}
		};

		
	  	
	        
	     protected void setAttributeSet(AttributeSet attr,
			boolean setParagraphAttributes) {
			if (m_skipUpdate)
				return;
			int xStart = m_editor.getSelectionStart();
			int xFinish = m_editor.getSelectionEnd();
			if (!m_editor.hasFocus()) {
				xStart = m_xStart;
				xFinish = m_xFinish;
			}

			if (setParagraphAttributes)
				m_doc.setParagraphAttributes(xStart,
					xFinish - xStart, attr, false);
			else if (xStart != xFinish)
				m_doc.setCharacterAttributes(xStart,
					xFinish - xStart, attr, false);
			else {
				MutableAttributeSet inputAttributes =
					m_kit.getInputAttributes();
				inputAttributes.addAttributes(attr);
			}
		}
	 
	     
	     protected String inputURL(String prompt, String initialValue) {
	 		JPanel p = new JPanel();
	 		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
	 		p.add(new JLabel(prompt));
	 		p.add(Box.createHorizontalGlue());
	 		JButton bt = new JButton("Local File");
	 		bt.setRequestFocusEnabled(false);
	 		p.add(bt);

	 		final JOptionPane op = new JOptionPane(p,
	 			JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
	 		op.setWantsInput(true);
	 		if (initialValue != null)
	 			op.setInitialSelectionValue(initialValue);

	 		ActionListener lst = new ActionListener() {
	 			public void actionPerformed(ActionEvent e) {
	 				JFileChooser chooser = new JFileChooser();
	 				if (chooser.showOpenDialog(HtmlEditor.this) !=
	 					JFileChooser.APPROVE_OPTION)
	 					return;
	 				File f = chooser.getSelectedFile();
	 				try {
	 					String str = f.toURI().toURL().toString();
	 					op.setInitialSelectionValue(str);
	 				}
	 				catch (Exception ex) {
	 					ex.printStackTrace();
	 				}
	 			}
	 		};
	 		bt.addActionListener(lst);

	 		JDialog dlg = op.createDialog(this, APP_NAME);
	 		dlg.setVisible(true);;
	 		dlg.dispose();

	 		Object value = op.getInputValue();
	 		if(value == JOptionPane.UNINITIALIZED_VALUE)
	 			return null;
	 		String str = (String)value;
	 		if (str != null && str.length() == 0)
	 			str = null;
	 		return str;}
	     
		public HtmlEditor() {
		
		initComponents();
		
		GraphicsEnvironment ge = GraphicsEnvironment.
				getLocalGraphicsEnvironment();
			String[] fontNames = ge.getAvailableFontFamilyNames();
	                m_cbFonts.setModel(new javax.swing.DefaultComboBoxModel(fontNames));
	    
	                m_htmlFilter = new SimpleFilter("html", "HTML Documents");

	                m_kit = new CustomHTMLEditorKit();
	        	    
	                m_editor.setEditorKit(m_kit);
	                
	                m_chooser = new JFileChooser();
	        		
	        	    m_chooser.setFileFilter(m_htmlFilter);
	        		try {
	        			File dir = (new File(".")).getCanonicalFile();
	        			m_chooser.setCurrentDirectory(dir);
	        		} catch (IOException ex) {}
	                
	                
	                   CaretListener lst;
	                   lst = new CaretListener() {
	                   public void caretUpdate(CaretEvent e) {
	                       showAttributes(e.getDot());
	                   }
	                   };
	                   
	        		m_editor.addCaretListener(lst);

	                        
	        		FocusListener flst = new FocusListener() {
	        			public void focusGained(FocusEvent e) {
	        				int len = m_editor.getDocument().getLength();
	        				if (m_xStart>=0 && m_xFinish>=0 && m_xStart<len && m_xFinish<len)
	        					if (m_editor.getCaretPosition()==m_xStart) {
	        						m_editor.setCaretPosition(m_xFinish);
	        						m_editor.moveCaretPosition(m_xStart);
	        					}
	        					else
	        						m_editor.select(m_xStart, m_xFinish);
	        			}

	        			public void focusLost(FocusEvent e) {
	        				m_xStart = m_editor.getSelectionStart();
	        				m_xFinish = m_editor.getSelectionEnd();
	        			}
	        		};
	        		m_editor.addFocusListener(flst);       
	                        
	        		newDocument();
	        		
	        		WindowListener wndCloser = new WindowAdapter() {
	        			public void windowClosing(WindowEvent e) {
	        				if (!promptToSave())
	        					return;
	        				System.exit(0);
	        			}
	        			public void windowActivated(WindowEvent e) {
	        				m_editor.requestFocus();
	        			}
	        		};
	        		addWindowListener(wndCloser);
		
	}

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {                                         
    try {
				StringWriter sw = new StringWriter();
				m_kit.write(sw, m_doc, 0, m_doc.getLength());
				sw.close();

				HtmlSourceDlg dlg = new HtmlSourceDlg(
					HtmlEditor.this, sw.toString());
				dlg.setVisible(true);
				if (!dlg.succeeded())
					return;

				StringReader sr = new StringReader(dlg.getSource());
				m_doc = (MutableHTMLDocument)m_kit.createDocument();
				m_context = m_doc.getStyleSheet();
				m_kit.read(sr, m_doc, 0);
				sr.close();
				m_editor.setDocument(m_doc);
				documentChanged();
			}
			catch (Exception ex) {
				showError(ex, "Error: "+ex);
			}
	} 
	
	 private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {                                         
	       if (!promptToSave())
		return;
	        newDocument();
	    }                                        

	 private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {                                         
		  if (!promptToSave())
			return;
			openDocument();  
		    } 
	
	 private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {                                         
	        saveFile(false);
	    } 
	 
	 private void m_cbSizesActionPerformed(java.awt.event.ActionEvent evt) {                                          
	       int fontSize = 0;
					try {
						fontSize = Integer.parseInt(m_cbSizes.
							getSelectedItem().toString());
					}
					catch (NumberFormatException ex) { return; }

					m_fontSize = fontSize;
					MutableAttributeSet attr = new SimpleAttributeSet();
					StyleConstants.setFontSize(attr, fontSize);
					setAttributeSet(attr);
					m_editor.grabFocus();
	     
	    } 
	 
	  private void m_cbFontsActionPerformed(java.awt.event.ActionEvent evt) {                                          
		     m_fontName = m_cbFonts.getSelectedItem().toString();
						MutableAttributeSet attr = new SimpleAttributeSet();
						StyleConstants.setFontFamily(attr, m_fontName);
						setAttributeSet(attr);
						m_editor.grabFocus();
		    }  
	  
	  private void m_bBoldActionPerformed(java.awt.event.ActionEvent evt) {                                        
	        MutableAttributeSet attr = new SimpleAttributeSet();
	 				StyleConstants.setBold(attr, m_bBold.isSelected());
	 				setAttributeSet(attr);
	 				m_editor.grabFocus();
	     } 
	  
	  private void m_bItalicActionPerformed(java.awt.event.ActionEvent evt) {                                          
		     MutableAttributeSet attr = new SimpleAttributeSet();
						StyleConstants.setItalic(attr, m_bItalic.isSelected());
						setAttributeSet(attr);
						m_editor.grabFocus();
		    } 
	  
	  private void m_bULActionPerformed(java.awt.event.ActionEvent evt) {                                      
	       MutableAttributeSet attr = new SimpleAttributeSet();
					StyleConstants.setUnderline(attr, m_bUL.isSelected());
					setAttributeSet(attr);
					m_editor.grabFocus();
	    }  
	  
	  private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {                                           
		    if (!promptToSave())
			return;
		        newDocument();
		    }                                          

	  private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {                                           
		    if (!promptToSave())
			return;
			openDocument();
	       }                                          

	  private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {                                           
		    saveFile(false);
		    }                                          

	  private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {                                           
		    saveFile(true);
		    }                                          

	  private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {                                           
		        System.exit(0);
		    } 
	  
	  private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {                                           
		     String url = inputURL("Please enter image URL:", null);
						if (url == null)
							return;
						try {
							ImageIcon icon = new ImageIcon(new URL(url));
							int w = icon.getIconWidth();
							int h = icon.getIconHeight();
							if (w<=0 || h<=0) {
								JOptionPane.showMessageDialog(HtmlEditor.this,
									"Error reading image URL\n"+
									url, APP_NAME,
									JOptionPane.WARNING_MESSAGE);
									return;
							}
							MutableAttributeSet attr = new SimpleAttributeSet();
							attr.addAttribute(StyleConstants.NameAttribute, HTML.Tag.IMG);
							attr.addAttribute(HTML.Attribute.SRC, url);
							attr.addAttribute(HTML.Attribute.HEIGHT, Integer.toString(h));
							attr.addAttribute(HTML.Attribute.WIDTH, Integer.toString(w));
							int p = m_editor.getCaretPosition();
							m_doc.insertString(p, " ", attr);
						}
						catch (Exception ex) {
							showError(ex, "Error: "+ex);
						}
		    }     
	  
	  private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {                                           
	      String oldHref = null;
					// The following code is correct, but may modify the original HTML - very strange...
					int p = m_editor.getCaretPosition();
					AttributeSet attr = m_doc.getCharacterElement(p).
						getAttributes();
					AttributeSet anchor = (AttributeSet)attr.getAttribute(HTML.Tag.A);
					if (anchor != null)
						oldHref = (String)anchor.getAttribute(HTML.Attribute.HREF);

					String newHref = inputURL("Please enter link URL:", oldHref);
					if (newHref == null)
						return;

					SimpleAttributeSet attr2 = new SimpleAttributeSet();
					attr2.addAttribute(StyleConstants.NameAttribute, HTML.Tag.A);
					attr2.addAttribute(HTML.Attribute.HREF, newHref);
					setAttributeSet(attr2, true);
					m_editor.grabFocus();
	    }    
	  
	  private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {                                           
	        DocumentPropsDlg dlg = new DocumentPropsDlg(HtmlEditor.this, m_doc);
					dlg.setVisible(true);;
					if (dlg.succeeded())
						documentChanged();
	    } 
}
