

public class HtmlWebDev {

	public static void main(String[] args) {
		 System.out.println("WebDev For Everyone");
		 try {
	            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
	                if ("Nimbus".equals(info.getName())) {
	                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
	                    break;
	                }
	            }
	        } catch (ClassNotFoundException ex) {
	            java.util.logging.Logger.getLogger(HtmlEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
	        } catch (InstantiationException ex) {
	            java.util.logging.Logger.getLogger(HtmlEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
	        } catch (IllegalAccessException ex) {
	            java.util.logging.Logger.getLogger(HtmlEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
	        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
	            java.util.logging.Logger.getLogger(HtmlEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
	        }
		 java.awt.EventQueue.invokeLater(new Runnable() {
	            public void run() {
	            	HtmlEditor window1=new HtmlEditor();
	            	window1.setSize(1000, 700);
	            	window1.setVisible(true);
	            	
	            }
	        });

	}


}
