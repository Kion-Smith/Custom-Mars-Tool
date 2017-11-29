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

import sun.audio.*;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
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
	private JButton tempButton,tempButton2;
	
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
	
	@Override
	protected JComponent buildMainDisplayArea() 
	{
		
		try
		{
			File music = new File("Hall_of_the_Mountain_King.wav");
			Clip clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(music));
		
		
	    	   JPanel displayArea = new JPanel();
	    	   tempButton = new JButton("TEST");
	    	   tempButton2 = new JButton("Stop");
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
	    	   
	    	   gc.gridx =4;
	    	   gc.gridy =1;
	    	   displayArea.add(tempButton2,gc);
	    	   
	    	   tempButton.addActionListener( new ActionListener()
	    		{
	    			   
					@Override
					public void actionPerformed(ActionEvent e) 
	    	   		{
						if(e.getSource().equals(tempButton))
						{
							music(0,clip);
						}
						
					}
	    			   
	    			   
	    		});
	    	   tempButton2.addActionListener( new ActionListener()
	    		{
	    			   
					@Override
					public void actionPerformed(ActionEvent e) 
	    	   		{
						if(e.getSource().equals(tempButton2))
						{
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
		return null;
	}
	public static void music(int x,Clip clip)
	{
		try
		{

	
			if( x == 0) 
			{
				clip.start();
			}
			else if(x ==1)
			{	
					//Thread.sleep(clip.getMicrosecondLength()/1000);		
				clip.stop();
				//clip.close();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		/*
		AudioPlayer mpg =  AudioPlayer.player;
		AudioStream bgm;
		AudioData md;
		
		ContinuousAudioDataStream loop = null;
		
		try 
		{
			bgm = new AudioStream(new FileInputStream("allen_arrogh.wav"));
			md = bgm.getData();
			loop = new ContinuousAudioDataStream(md);
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		mpg.start(loop);
		*/
		
	}

	

}
