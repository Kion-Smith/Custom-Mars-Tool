/*

Copyright 2014-15, James Hester.

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

package mars.tools;
import javax.swing.*;

import mars.Globals;
import mars.ProgramStatement;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.hardware.RegisterAccessNotice;
import mars.mips.hardware.RegisterFile;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Observable;

		
   /**
	 * Visual Stack: a tool providing a simple graphical display of the stack.
	 * By James Hester, 2014.
	 */
   /**
     * Minor modifications by Alexander Pabst, 2015
     */
    public class VisualStack extends AbstractMarsToolAndApplication 
    {
    	
    	private static String name = "Visual Stack";
    	private static String heading =  "Visual Stack";
    	private static String version = "Version 1.0";
    	
    	private static final int STACK_VIEWER_WIDTH = 400;
    	private static final int STACK_VIEWER_HEIGHT = 400;
    	private static final int STACK_VIEWER_NUM_COLS = 10;
    	private static final int STACK_VIEWER_NUM_ROWS = 10;
    	
    	private JScrollPane displayAreaScrollPane;
    	private StackViewer theStackViewer;
    	private JLabel statusLabel;
    	
    	private int oldStackPtrValue;
    	private boolean stackOK = true;
    	
   	         	
   	/**
   	 * Simple constructor, likely used to run the tool as a stand-alone app.
   	 * @param title String containing title for title bar
   	 * @param heading String containing text for heading shown in upper part of window.
   	 */
       public VisualStack(String title, String heading) {
         super(title,heading);
      }
   	 
   	 /**
   	  *  Simple constructor, likely used by the MARS Tools menu mechanism
   	  */
       public VisualStack() {
         super (heading+", "+version, heading);
      }
   		 
   		 
   	/**
   	 * Main provided for pure stand-alone use.
   	 */
       public static void main(String[] args) {
         new VisualStack(heading+", "+version,heading).go();
      }
   	
   
     /**
   	  *  Required method to return Tool name.  
   	  *  @return  Tool name.  MARS will display this in menu item.
   	  */
       public String getName() {
         return name;
      }
       
       
       @Override
       public void initializePreGUI()
       {
    	   /*
    	    * As far as memory goes, we want to look at the stack, and only the stack.
    	    */
    	   this.addAsObserver(Memory.stackLimitAddress, Memory.stackBaseAddress);
    	   /*
    	    * But we also want to look at $sp.
    	    */
    	   this.addAsObserver(RegisterFile.getUserRegister("$sp"));
    	   
    	   this.oldStackPtrValue = Memory.stackPointer;
       }
   	
    /**
   	 *  The display area of the tool consists of a StackViewer object contained within a JScrollPane:
   	 *  the StackViewer component grows horizontally as the stack itself grows. A JLabel provides basic
   	 *  status information and some terse error messages.
   	 */
       protected JComponent buildMainDisplayArea() 
       {
    	   JPanel displayArea = new JPanel();
    	   displayArea.setLayout(new BoxLayout(displayArea, BoxLayout.PAGE_AXIS));
    	   
    	   theStackViewer = new StackViewer(STACK_VIEWER_WIDTH, STACK_VIEWER_HEIGHT);
    	   statusLabel = new JLabel("Stack status: OK");
    	   
    	   //The StackViewer will never grow horizontally...never scroll horizontally.
    	   //The vertical scrollbar is there to remind the user to scroll up and down to see the whole thing.
    	   displayAreaScrollPane = new JScrollPane(theStackViewer,
    			   									 ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, 
    			   									 ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    	   
    	   
    	   
    	   displayArea.add(displayAreaScrollPane);
    	   displayArea.add(statusLabel);
    	   
    	   //Center the status label: I don't think this should be necessary
    	   //because of BoxLayout, but apparently it is.
    	   statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    	   
    	   return displayArea;
       }
       
       /**
        * All we need to do after the GUI is set up is make sure the user can't resize the window.
        */
       @Override
       public void initializePostGUI()
       {
    	   /*
    	    * As of MARS 4.4, if we're a tool in the IDE, theWindow is a JDialog containing
    	    * the tool. If we're a standalone application, theWindow = this, 
    	    * which is a superclass of a JFrame. Preventing the user from resizing the window
    	    * must be handled in two separate ways.
    	    */
			if (this.isBeingUsedAsAMarsTool)
			{
				((JDialog) theWindow).setResizable(false);
			}
			else
			{
				this.setResizable(false);
			}
       }
       
       /**
        * getHelpComponent() as implemented here is adapted from code in KeyboardAndDisplaySimulator.java,
        * written by Pete Sanderson and Kenneth Vollmar.
        * Indeed, only the help text and size of the JTextArea have been changed, along with making
        * the JTextArea noneditable.
        */
       @Override
	   protected JComponent getHelpComponent()
	   {
    	   final String helpContent = name + ", " + version
    	   		+ "\n\n"
    	   		+ "This tool provides a graphical visualization of the MIPS stack. "
    	   		+ "When data is pushed to the stack, a colored rectangle representing the data appears in the appropriate position, along with information about which register the data came from. "
    	   		+ "Visual Stack was written to aid developers of recursive functions in debugging and to help them avoid common stack pitfalls; as such, it distinguishes between return addresses and other types of data. "
    	   		+ "(To do this, it uses a heuristic: if the data is the address of a jal command plus 4, it is probably a return address. This properly identifies return addresses contained in registers other than $ra.) "
    	   		+ "Aside from showing data on the stack, Visual Stack represents the position of the stack pointer both graphically (a green arrow points to the data at $sp) and textually (the value of $sp is displayed.) "
    	   		+ "\n\n"
    	   		+ "Visual Stack can also detect certain error conditions, such as $sp not being aligned on a word boundary. Should such a condition arise, the screen will freeze and the error will be briefly described. "
    	   		+ "The stack will not be updated again until the Reset button is pressed. "
    	   		+ "\n\n"
    	   		+ "This tool was written by James Hester, a student at the University of Texas at Dallas, in November 2014.";
    	   JButton help = new JButton("Help");
    	   help.addActionListener(new ActionListener()
    	   {
    		   public void actionPerformed(ActionEvent e)
    		   {
	   				JTextArea ja = new JTextArea(helpContent);
	   				ja.setRows(15);
	   				ja.setColumns(30);
	   				ja.setLineWrap(true);
	   				ja.setWrapStyleWord(true);
	   				ja.setEditable(false);
	   				JOptionPane.showMessageDialog(theWindow, new JScrollPane(ja),
	   						"Visual Stack",
	   						JOptionPane.INFORMATION_MESSAGE);
	   			}
	   		});
	   		return help;
	   	}
       
   		@Override
   		protected void processMIPSUpdate(Observable resource, AccessNotice notice)
   		{
   			/*
   			 * For some reason, the tool would get RegisterAccessNotices after it was disconnected,
   			 * making the stack pointer indicator move (but nothing would show up in the registers.)
   			 * So, manually check if the tool is connected to MARS.
   			 */
   			if (stackOK && this.isObserving()) //Do nothing if an error occurred
   			{
   			synchronized (Globals.memoryAndRegistersLock)
   			{
	   			if (notice.getAccessType() == AccessNotice.WRITE)
	   			{
	   				if (notice.accessIsFromGUI())
	   				{
	   					return;
	   				}
	   				if (notice instanceof MemoryAccessNotice)
	   					processMemoryUpdate((MemoryAccessNotice) notice);
	   				else
	   					processStackPtrUpdate((RegisterAccessNotice) notice);	
	   			}
   			}
   			}
   		}
   		
   		private void processMemoryUpdate(MemoryAccessNotice theNotice)
   		{
   			String description;
   			int address = theNotice.getAddress();
   			int valueWritten = 0;
   			int registerDataCameFrom = 0;
   			/*
   			 * Not sure why this is necessary. We are observing only the memory between these two addresses anyway.
   			 * Nevertheless, before this check was here, certain memory writes to non-stack locations would trigger this function,
   			 * leading to confusing garbage.
   			 */
   			if (address > Memory.stackBaseAddress || address < Memory.stackLimitAddress)
   				return;

   			try
			{
   				/*
   				 * The stack is written to by the instruction sw $rd, X($sp).
   				 * In this block, we go to the text segment to find the particular sw instruction
   				 * that wrote to the stack (it is at location PC - 4, because the PC has been advanced already).
   				 * Given this instruction, we look at bits 21-17 to find the value of $rd.
   				 * This, as a number from 0-31, is placed in registerDataCameFrom.
   				 */
				valueWritten = Memory.getInstance().getWord(address);
				ProgramStatement instr = Memory.getInstance().getStatement(RegisterFile.getProgramCounter() - 4);
				registerDataCameFrom = instr.getBinaryStatement();
				registerDataCameFrom &= 0x001F0000;
				registerDataCameFrom >>= 16;
			
			} catch (AddressErrorException e)
			{
				e.printStackTrace();
				return;
			}
   			
   			int position = (Memory.stackPointer - address) / 4;
   			if (position < 0)
   			{
   				handleError("Data was pushed to an address greater than stack base!");
   				return;
   			}

   			boolean dataIsReturnAddress = isReturnAddress(valueWritten);
   			
   			if (dataIsReturnAddress)
   				description = "Return address ";
   			else
   				description = "Data ";
   			description += "from register ";
   			description += RegisterFile.getRegisters()[registerDataCameFrom].getName(); //Converts int in range 0-31 to register name as String
   			
   			if(position > 0) {
   				theStackViewer.insertStackElement(position, dataIsReturnAddress, description);
   			}
   		}
   		
   		private void handleError(String errorText)
   		{
			statusLabel.setForeground(Color.RED);
			statusLabel.setText(errorText); //Show the error message
			
			theStackViewer.errorOverlay(); //As if everything else wasn't enough, make the stack bloody red
			Toolkit.getDefaultToolkit().beep(); //"Ding" (tested on Windows and OS X, uses system sounds on both)
			stackOK = false;
			
			repaint(); //theStackViewer redraws itself...but the status label does not! So repaint() is called.
   		}
   		
   		private void processStackPtrUpdate(RegisterAccessNotice notice)
   		{
   			int newStackPtrValue = RegisterFile.getValue(29);
   			   			
   			int stackPtrDelta = oldStackPtrValue - newStackPtrValue;
   			
   			if (stackPtrDelta % 4 != 0)
   			{
   				handleError("$sp set to 0x" + Integer.toHexString(newStackPtrValue) + "; not word-aligned!");
   				return;
   			}
   			
   			stackPtrDelta /= 4;
   			
   			//System.out.println("The stack pointer was advanced (# positions): " + stackPtrDelta);
   			
   			theStackViewer.advanceStackPointer(stackPtrDelta);
   			
   			oldStackPtrValue = newStackPtrValue;
   		}
       
   		/**
   		 * Use a simple heuristic to guess whether an arbitrary 32-bit integer is a return address.
   		 * For the sake of this method, return addresses are generated by the "jal" instruction (and by this operation
   		 * alone) and they point to the instruction after this one.
   		 * @param theData the potential return address
   		 * @return guess whether theData is a return address
   		 */
   		private synchronized boolean isReturnAddress(int theData)
   		{
   			if (Memory.inTextSegment(theData))
   			{
   				try 
   				{
					String theStatement = Memory.getInstance().getStatement(theData - 4).getBasicAssemblyStatement();
					if (theStatement.substring(0, 3).equals("jal"))
						return true;
					else
						return false;
				} 
   				catch (Exception e)
   				{
					return false;
				}
   			}
   			return false;
   		}
   		
   		@Override
   		public void reset()
   		{
     	   theStackViewer.reset(STACK_VIEWER_WIDTH, STACK_VIEWER_HEIGHT);
     	   statusLabel.setForeground(Color.BLACK);
     	   statusLabel.setText("Stack status: OK");
     	   
     	   theStackViewer.advanceStackPointer(0);
     	   this.oldStackPtrValue = Memory.stackPointer;
     	        	   
     	   stackOK = true;
     	   
     	   this.repaint();
   			
   		}
   		
   		/**
   		 * The component which actually draws the stack.
   		 * Basically a wrapper around a BufferedImage, StackViewer contains all
   		 * of the important graphical code, metrics, etc. in VisualStack.
   		 * @author James Hester
   		 * @version 1.0
   		 *
   		 */
       private class StackViewer extends JComponent
       {
       		private BufferedImage screen;
       		private Graphics2D imageWriter;
       		
       		private int stackPtrPosition = 0;
       		private int highestOccupiedPosition;
       		
       		private Color garbageDark = new Color(0x801515);
       		private Color garbageLight = new Color(0xFFAAAA);
       		
       		private Color dataDark = new Color(0x0D4D4D);
       		private Color dataLight = new Color(0x669999);
       		
       		private Color retDark = new Color(0x567714);
       		private Color retLight = new Color(0xD4EE9F);
       		
       		private Color nullDark = new Color(0x424242);
       		private Color nullLight = new Color(0xD2D2D2);
       		
       		public StackViewer(int width, int height)
       		{
       			super();
       			reset(width, height);
       		}
       		
       		public void reset(int width, int height)
       		{
       			super.setSize(width, height);
       			super.setPreferredSize(new Dimension(width, height));
       			screen = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
       			imageWriter = screen.createGraphics();
       			
       			imageWriter.setFont(new Font("SansSerif", Font.BOLD, 14));
       			
       			imageWriter.setColor(Color.BLACK);
       			imageWriter.fillRect(0, 0, width, height);
       			
       			/*
       			 * Draw labels and graphics: the stack container, $sp, etc.
       			 */
       			
       			
       			//"Bottom" of stack
       			//imageWriter.setColor(Color.WHITE);
       			//imageWriter.fillRect(38, 2, 4, 34);
       			//imageWriter.fillRect(38, 2, width - 40, 4);
       			//imageWriter.fillRect(width - 6, 2, 4, 34);
       			
       			//Grid lines
       			imageWriter.setColor(nullLight);
       			imageWriter.drawLine(0, height / STACK_VIEWER_NUM_ROWS, width, height / STACK_VIEWER_NUM_ROWS);
       			imageWriter.drawLine(STACK_VIEWER_WIDTH / STACK_VIEWER_NUM_COLS, height / STACK_VIEWER_NUM_ROWS, STACK_VIEWER_WIDTH / STACK_VIEWER_NUM_COLS, height);
       			       			
       			stackPtrPosition = 0;
       			highestOccupiedPosition = 0;
       			advanceStackPointer(0);
       			//insertEmptyElement(0);
       		}
       		
       		public void printStackPointerAddress(int position)
       		{
       			String spAddress = "0x" + Integer.toHexString(Memory.stackPointer - (position * 4));
       			imageWriter.setColor(Color.BLACK);
       			imageWriter.fillRect(0, 0, STACK_VIEWER_WIDTH, 39);
       			imageWriter.setColor(Color.WHITE);
       			imageWriter.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
       			printStringCentered("$sp: " + spAddress, 116, 0, 24);
       		}
       		
       		
       		public void advanceStackPointer(int numPositions)
       		{
       			int newPosition = stackPtrPosition + numPositions;
       			
       			printStackPointerAddress(newPosition);
       			
       			ensureHeight(STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS * (newPosition + 1));
       			
       			imageWriter.setColor(Color.BLACK);
       			imageWriter.fillRect(0, STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS * (stackPtrPosition > 0 ? stackPtrPosition : 1/*+ 1*/) + 1, 35, 39);
       			
       			drawArrow(newPosition, Color.GREEN);
       			
       			if (numPositions < 0)
       			{
       				for(int i = newPosition + 1; i <= stackPtrPosition; i++)
       					removeStackElement(i);
       			}
       			
       			else if (numPositions > 0 && newPosition > highestOccupiedPosition)
       			{
       				for(int i = highestOccupiedPosition + 1; i <= newPosition; i++) {
       					insertEmptyElement(i);
       				}
       			}
       			
       			
       			repaint();
       			
       			stackPtrPosition = newPosition;
       		}
       		
       		public void insertEmptyElement(int which)
       		{
       			int y1 = 2 + (STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS * (which));
       			
       			//First, draw the box.
       			imageWriter.setColor(nullDark);
       			imageWriter.fillRect((STACK_VIEWER_WIDTH / STACK_VIEWER_NUM_COLS) + 2, y1, 320, (STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS) - 4);
       			imageWriter.setColor(nullLight);     			
       			imageWriter.fillRect((STACK_VIEWER_WIDTH / STACK_VIEWER_NUM_COLS) + 6, y1 + 4, 320 - 8, (STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS) - 12);
       			
       			//Now, print the string.
       			imageWriter.setColor(Color.BLACK);
       			int strHeight = (int)imageWriter.getFontMetrics().getLineMetrics("Null or invalid data", imageWriter).getHeight();
       			printStringCentered("Null or invalid data", 312, 42, y1 + 28 - ((28 - strHeight) / 2) );       			
       		}
       		
       		public void removeStackElement(int which)
       		{
       			int y1 = 1 + (STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS * (which));
       			imageWriter.setColor(Color.BLACK);
       			imageWriter.fillRect((STACK_VIEWER_WIDTH / STACK_VIEWER_NUM_COLS) + 1, y1, 320 + 2, (STACK_VIEWER_WIDTH / STACK_VIEWER_NUM_COLS) + 2);
       			highestOccupiedPosition = which - 1;
       		}
       		
       		public void insertStackElement(int which, boolean isReturnAddress, String label)
       		{
       			//System.out.println(which);
       			
       			if (which > highestOccupiedPosition)
       				highestOccupiedPosition = which;
       			
       			ensureHeight(STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS * (which + 1));
       			
       			int y1 = 2 + (STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS * (which));
       			
       			//First, draw the box.
       			if (isReturnAddress)
       				imageWriter.setColor(retDark);
       			else
       				imageWriter.setColor(dataDark);
       			imageWriter.fillRect((STACK_VIEWER_WIDTH / STACK_VIEWER_NUM_COLS) + 2, y1, 320, (STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS) - 4);
       			
       			if (isReturnAddress)
       				imageWriter.setColor(retLight);
       			else
       				imageWriter.setColor(dataLight);     			
       			imageWriter.fillRect((STACK_VIEWER_WIDTH / STACK_VIEWER_NUM_COLS) + 6, y1 + 4, 320 - 8, (STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS) - 12);
       			
       			//Now, print the string.
       			imageWriter.setColor(Color.BLACK);
       			int strHeight = (int)imageWriter.getFontMetrics().getLineMetrics(label, imageWriter).getHeight();
       			printStringCentered(label, 312, 42, y1 + 28 - ((28 - strHeight) / 2) );
       			
       			repaint();
       		}
       		
       		public void ensureHeight(int newHeight)
       		{
       			//System.out.println(newHeight);
       			if (newHeight > screen.getHeight())
       			{
	       			BufferedImage newScreen = new BufferedImage(screen.getWidth(), newHeight, BufferedImage.TYPE_4BYTE_ABGR);
	       			Graphics2D newIW = newScreen.createGraphics();
	       			
	       			//Make the new ImageWriter as much like the old one as possible.
	       			newIW.drawImage(screen, 0, 0, this);
	       			
	       			newIW.setColor(Color.BLACK);
	       			newIW.fillRect(0, screen.getHeight(), screen.getWidth(), newHeight - screen.getHeight()); //Keep the black background going
	       			newIW.setColor(nullLight);
	       			newIW.drawLine(STACK_VIEWER_WIDTH / STACK_VIEWER_NUM_COLS, screen.getHeight(), STACK_VIEWER_WIDTH / STACK_VIEWER_NUM_COLS, newHeight); //Draw the gray line separating the arrow and the data

	       			newIW.setColor(imageWriter.getColor());
	       			newIW.setFont(imageWriter.getFont());
	       			newIW.setRenderingHints(imageWriter.getRenderingHints());
	       			
	       			screen = newScreen;
	       			imageWriter = newIW;
	       			
	       			this.setPreferredSize(new Dimension(this.getWidth(), newHeight));
	       			
	       			revalidate();
	       			repaint();
       			}
       		}
       		
       		private void printStringCentered(String str, int width, int x, int y)
       		{
       			int offset = imageWriter.getFontMetrics().stringWidth(str);
       			offset = (width - offset) / 2;
       			imageWriter.setFont(new Font("SansSerif", Font.BOLD, 14));
       			imageWriter.drawString(str, x + offset, y);
       		}
       		
       		private void drawArrow(int position, Color color)
       		{
       			int[] arrowXPoints = {12, 24, 12};
       			int[] arrowYPoints = {10, 20, 30};
       			
       			for(int i = 0; i < arrowYPoints.length; i++)
       			{
       				arrowYPoints[i] += ((position > 0 ? position : 1) * STACK_VIEWER_HEIGHT / STACK_VIEWER_NUM_ROWS);
       			}
       			
       			imageWriter.setColor(color);
       			imageWriter.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
       			imageWriter.fillPolygon(arrowXPoints, arrowYPoints, 3);       			
       		}
       		
       		public void errorOverlay()
       		{
       			imageWriter.setColor(new Color(255, 0, 0, 128));
       			imageWriter.fillRect(0, 0, this.getSize().width, this.getSize().height);
       			repaint();
       		}
       		       		
    		@Override
    		public void paintComponent(Graphics g)
    		{
    			super.paintComponent(g);
    			if (screen != null)
    			{
    				g.drawImage(screen, 0, 0, this);
    			}
    		}
    		    	   
       }
   }