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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Observable;

import sun.audio.*;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileSystemView;

import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.Memory;


public class MusicPlayer extends AbstractMarsToolAndApplication
{

	private static String name = "Music Player";
	private static String heading =  "Music Player";
	private static String version = "Version 1";
	
	

	private JLabel statusLabel;
	private JTextField musicTextField;
	private JButton playButton,stopButton;
	
	private static boolean isPlaying = false;
	

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
	
	
    public static void main(String[] args)
    {
        new MusicPlayer(heading+", "+version,heading).go();
    }
  	
	
	@Override
	protected JComponent buildMainDisplayArea() 
	{
 	   JPanel displayArea = new JPanel();
 	   playButton = new JButton("Play");
 	   stopButton = new JButton("Stop");
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
 	   displayArea.add(playButton,gc);
 	   
 	   gc.gridx =4;
 	   gc.gridy =1;
 	   displayArea.add(stopButton,gc);
		
		try
		{


			URL music = this.getClass().getResource("/resource/Hall_of_the_Mountain_King.wav");
			
			Clip clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(music));
		
		

	    	   
	    	   playButton.addActionListener( new ActionListener()
	    		{
	    			   
					@Override
					public void actionPerformed(ActionEvent e) 
	    	   		{
						if(e.getSource().equals(playButton))
						{
							music(0,clip);
							musicTextField.setText(" Song is playing ");
						}
						
					}
	    			   
	    			   
	    		});
	    	   stopButton.addActionListener( new ActionListener()
	    		{
	    			   
					@Override
					public void actionPerformed(ActionEvent e) 
	    	   		{
						if(e.getSource().equals(stopButton))
						{
							musicTextField.setText(" Song is stopped ");
							music(1,clip);
						}
						
					}
	    			   
	    			   
	    		});
	    	   
	    	   return displayArea;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return displayArea;
	}
	public static void music(int x,Clip clip)
	{
		try
		{
	
			if( x == 0) 
			{
				clip.start();
				clip.loop(Clip.LOOP_CONTINUOUSLY);
			}
			else if(x ==1)
			{		
				clip.stop();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	
		
	}

	

}
