/*
 * Created by Kion Smith
 * 
 * 
 */


package mars.tools;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import sun.audio.*;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;


public class MusicPlayer extends AbstractMarsToolAndApplication
{

	private static String name = "Music Player";
	private static String heading =  "Music Player";
	private static String version = "Version 1";
	
	

	private JLabel statusLabel;
	private JTextField musicTextField;
	private JButton tempButton;
	
	
	protected MusicPlayer(String title, String heading) 
	{
		super(title, heading);
		
	}
	 public MusicPlayer() 
	 {
	        super (heading+", "+version, heading);
	 }
	@Override
	public String getName()
	{
        return name;
     }
	
	@Override
	protected JComponent buildMainDisplayArea() 
	{
	    	   JPanel displayArea = new JPanel();
	    	   tempButton = new JButton("TEST");
	    	   statusLabel = new JLabel("Song:");
	    	   musicTextField = new JTextField(" No song playing ");
	    	   
	    	   displayArea.setLayout(new GridBagLayout());
	    	   GridBagConstraints gc = new GridBagConstraints();
	    	      
	    	   

	    	   gc.gridx =1;
	    	   gc.gridy =1;
	    	   displayArea.add(statusLabel,gc);
	    	   
	    	   gc.gridx =2;
	    	   gc.gridy =1;
	    	   displayArea.add(musicTextField,gc);
	    	   
	    	   gc.gridx =3;
	    	   gc.gridy =1;
	    	   displayArea.add(tempButton,gc);
	    	   
	    	   tempButton.addActionListener( new ActionListener()
	    		{
	    			   
					@Override
					public void actionPerformed(ActionEvent e) 
	    	   		{
						if(e.getSource().equals(tempButton))
						{
							System.out.print("WORKS");
						}
						
					}
	    			   
	    			   
	    		});
	    	   
	    	   return displayArea;
	}
	

}
