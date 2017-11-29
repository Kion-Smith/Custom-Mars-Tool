package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Observable;
import java.util.Random;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

import mars.Globals;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.simulator.Exceptions;
import mars.util.Binary;
import mars.venus.AbstractFontSettingDialog;

/*
 Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar
 Derivative work: copyright (c) 2014, W. James Hester

 Based on the Keyboard and Display Simulator written by 
 Pete Sanderson (psanderson@otterbein.edu) and
 Kenneth Vollmar (kenvollmar@missouristate.edu)

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

/**
 * Keyboard and Graphics Adapter Simulator. Identical in many ways to
 * the class on which it is based (the Keyboard and Display Simulator)<br>
 * Version 1.0, October 2014
 * 
 */
public class KeyboardAndGraphicsAdapterSimulator extends AbstractMarsToolAndApplication
{

	private static String version = "Version 1.0";
	private static String heading = "Keyboard and Graphics Adapter Simulator";
	public static Dimension preferredDisplayDimension = new Dimension(640, 480);
	private static Insets textAreaInsets = new Insets(4, 4, 4, 4);
	private static final int MAX_KEYBOARD_PANEL_HEIGHT = 64;
	
	public static int RECEIVER_CONTROL; // keyboard Ready in low-order bit
	public static int RECEIVER_DATA; // keyboard character in low-order byte
	public static int ADAPTER_STATUS; // display Ready in low-order bit
	public static int ADAPTER_DATA; // display character in low-order byte
	public static int DISPLAY_VRAM_BASE;
	public static int DISPLAY_VRAM_END;

	/*
	 * The sophisticated instruction counting facilities of the Keyboard 
	 * and Display Simulator were mostly discarded, because the original intent of
	 * this tool was simply to be as useful as possible as a text display at
	 * the expense of realism. The adapter, however, uses some fixed delays 
	 * (eg. to resize the screen always takes 7500 instructions).
	 */
	private boolean countingInstructions;
	private int instructionCount;
	private int transmitDelayInstructionCountLimit;

	// Major GUI components
	private JPanel keyboardAndDisplay;
	private DisplayPanel display;
	private JPanel keyboardPanel;
	private JTextField keyEventAccepter;
	private Font defaultFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);

	/**
	 * Simple constructor, likely used to run a stand-alone keyboard/display
	 * simulator.
	 * 
	 * @param title
	 *            String containing title for title bar
	 * @param heading
	 *            String containing text for heading shown in upper part of
	 *            window.
	 */
	public KeyboardAndGraphicsAdapterSimulator(String title, String heading)
	{
		super(title, heading);
	}

	/**
	 * Simple constructor, likely used by the MARS Tools menu mechanism
	 */
	public KeyboardAndGraphicsAdapterSimulator()
	{
		super(heading + ", " + version, heading);
	}

	/**
	 * Main provided for pure stand-alone use. Recommended stand-alone use is to
	 * write a driver program that instantiates a TerminalEmulator object then
	 * invokes its go() method. "stand-alone" means it is not invoked from the
	 * MARS Tools menu. "Pure" means there is no driver program to invoke the
	 * application.
	 */
	public static void main(String[] args)
	{
		new KeyboardAndGraphicsAdapterSimulator(heading + " stand-alone, " + version, heading)
				.go();
	}

	/**
	 * Required MarsTool method to return Tool name.
	 * 
	 * @return Tool name. MARS will display this in menu item.
	 */
	public String getName()
	{
		return heading;
	}

	// Set the MMIO addresses. Prior to MARS 3.7 these were final because
	// MIPS address space was final as well. Now we will get MMIO base address
	// each time to reflect possible change in memory configuration. DPS
	// 6-Aug-09
	protected void initializePreGUI()
	{
		RECEIVER_CONTROL = Memory.memoryMapBaseAddress; // 0xffff0000; keyboard control register
		RECEIVER_DATA = Memory.memoryMapBaseAddress + 4; // 0xffff0004; keyboard character stored in low byte
		ADAPTER_STATUS = Memory.memoryMapBaseAddress + 8; // 0xffff0008; display adapter's Status Register
		ADAPTER_DATA = Memory.memoryMapBaseAddress + 12; // 0xffff000c; display adapter's Command Register
	}

	/**
	 * Override the inherited method, which registers us as an Observer over the
	 * static data segment (starting address 0x10010000) only.
	 * 
	 * When user enters keystroke, set RECEIVER_CONTROL and RECEIVER_DATA using
	 * the action listener. When user loads word (lw) from RECEIVER_DATA (we are
	 * notified of the read), then clear RECEIVER_CONTROL. When user stores word
	 * (sw) to ADAPTER_DATA (we are notified of the write), control is handed
	 * off to the DisplayPanel class which takes the appropriate action.
	 * 
	 * If you use the inherited GUI buttons, this method is invoked when you
	 * click "Connect" button on MarsTool or the "Assemble and Run" button on a
	 * Mars-based app.
	 */
	protected void addAsObserver()
	{
		// Set transmitter Control ready bit to 1, means we're ready to accept
		// display character.
		updateMMIOControl(ADAPTER_STATUS, readyBitSet(ADAPTER_STATUS));
		// We want to be an observer only of MIPS reads from RECEIVER_DATA and
		// writes to TRANSMITTER_DATA.
		// Use the Globals.memory.addObserver() methods instead of inherited
		// method to achieve this.
		addAsObserver(RECEIVER_DATA, RECEIVER_DATA);
		addAsObserver(ADAPTER_DATA, ADAPTER_DATA);
		/*
		 * We want to be notified of each instruction execution, because
		 * instruction count is the basis for delay in re-setting 
		 * (literally) the ADAPTER_CONTROL register. SPIM does
		 * this too. This simulates the time required for the display unit to
		 * process the command.
		 */
		addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
		addAsObserver(Memory.kernelTextBaseAddress,
				Memory.kernelTextLimitAddress);
	}

	/**
	 * Method that constructs the main display area. It is organized vertically
	 * into two major components: the display and the keyboard. 
	 * 
	 * @return the GUI component containing these two areas
	 */
	protected JComponent buildMainDisplayArea()
	{
		try
		{
			keyboardAndDisplay = new JPanel();
			keyboardAndDisplay.setLayout(new BoxLayout(keyboardAndDisplay,
					BoxLayout.Y_AXIS));

			keyboardAndDisplay.add(buildDisplay());
			keyboardAndDisplay.add(buildKeyboard());
		} catch (Exception e)
		{
			System.err.println("Exception building main display area:");
			e.printStackTrace();
		}

		return keyboardAndDisplay;
	}

	// ////////////////////////////////////////////////////////////////////////////////////
	// Rest of the protected methods. These all override do-nothing methods
	// inherited from
	// the abstract superclass.
	// ////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Update display when connected MIPS program accesses (data) memory.
	 * 
	 * @param memory
	 *            the attached memory
	 * @param accessNotice
	 *            information provided by memory in MemoryAccessNotice object
	 */
	protected void processMIPSUpdate(Observable memory,
			AccessNotice accessNotice)
	{
		MemoryAccessNotice notice = (MemoryAccessNotice) accessNotice;
		// If MIPS program has just read (loaded) the receiver (keyboard) data register,
		// then clear the Ready bit to indicate there is no longer a keystroke available.
		// If Ready bit was initially clear, they'll get the old keystroke -- serves 'em right for not checking!
		if (notice.getAddress()==RECEIVER_DATA && notice.getAccessType()==AccessNotice.READ) 
		{
			updateMMIOControl(RECEIVER_CONTROL,
							  readyBitCleared(RECEIVER_CONTROL));
		}
		/*
		 * If the MIPS program writes to the whole adapter at once, OR if it writes
		 * to the Character Register (high byte of ADAPTER_DATA), a command is triggered!
		 * 
		 */
		if (	((notice.getAddress() == ADAPTER_DATA + 3 && notice.getLength() == 1)
				|| (notice.getAddress() == ADAPTER_DATA && notice.getLength() == 4))
				&& notice.getAccessType() == AccessNotice.WRITE
				&& isReadyBitSet(ADAPTER_STATUS)
				&& !this.countingInstructions)
		{
			try
			{
				int command = Memory.getInstance().getRawWord(ADAPTER_DATA);
				DisplayCommandResults r =	display.processCommand(command);
				this.transmitDelayInstructionCountLimit = r.getDelay();
				updateMMIOControl(ADAPTER_STATUS, r.getStatus());
			} catch (Exception e)
			{
				System.err.println("Exception occurred when processing command!\n\n");
				e.printStackTrace();
			}
			//Count instructions only if the instruction count limit is greater than zero.
			this.countingInstructions = this.transmitDelayInstructionCountLimit > 0;
			this.instructionCount = 0;
		}
		// We have been notified of a MIPS instruction execution.
		// If we are in transmit delay period, increment instruction count and
		// if limit
		// has been reached, set the transmitter Ready flag to indicate the MIPS
		// program
		// can write another character to the transmitter data register. If the
		// Interrupt-Enabled
		// bit had been set by the MIPS program, generate an interrupt!
		if (	this.countingInstructions
				&& notice.getAccessType() == AccessNotice.READ
				&& (Memory.inTextSegment(notice.getAddress())
					|| Memory.inKernelTextSegment(notice.getAddress()))
		   )
		{
			this.instructionCount++;
			if (this.instructionCount >= this.transmitDelayInstructionCountLimit)
			{
				this.countingInstructions = false;
				int updatedTransmitterControl = readyBitSet(ADAPTER_STATUS);
				updateMMIOControl(ADAPTER_STATUS,
						updatedTransmitterControl);
			   if ((updatedTransmitterControl & 0x00000002) == 2 //adapter: interrupts are enabled
				 && (Coprocessor0.getValue(Coprocessor0.STATUS) & 2)==0 // Added by Carl Hauser Nov 2008
				 && (Coprocessor0.getValue(Coprocessor0.STATUS) & 1)==1)
			    {
				   /*
				    * The interrupt-enabled bit is set in both Adapter Status (bit 2)
				    * and in Coprocessor0 Status register, and Interrupt Level 
				    * Bit is 0, so trigger external interrupt.
				    */
				 mars.simulator.Simulator.externalInterruptingDevice = Exceptions.EXTERNAL_INTERRUPT_DISPLAY;
				 }
			}
		}
	}

	/**
	 * Initialization code to be executed after the GUI is configured. Overrides
	 * inherited default.
	 */

	protected void initializePostGUI()
	{
		try 
		{
			if (this.isBeingUsedAsAMarsTool)
			{
				((JDialog) this.theWindow).setResizable(false);
			}
			else
			{
				this.setResizable(false);
			}
			display.reinit();
	
			initializeTransmitDelaySimulator();
			keyEventAccepter.requestFocusInWindow();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		doDebugCommands();
	}

	/**
	 * Method to reset counters and display when the Reset button selected.
	 * Overrides inherited method that does nothing.
	 */
	protected void reset()
	{
		initializeTransmitDelaySimulator();
		display = new DisplayPanel();
		display.setSize(preferredDisplayDimension);
		display.reinit();
		keyEventAccepter.setText("");
		keyEventAccepter.requestFocusInWindow();
		updateMMIOControl(ADAPTER_STATUS, readyBitSet(ADAPTER_STATUS));
	}

	/**
	 * Overrides default method, to provide a Help button for this tool/app.
	 */

	
	protected JComponent getHelpComponent()
	{
		final String helpContent = "";
		JButton help = new JButton("Help");
		help.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JTextArea ja = new JTextArea(helpContent);
				ja.setRows(30);
				ja.setColumns(60);
				ja.setLineWrap(true);
				ja.setWrapStyleWord(true);
				JOptionPane.showMessageDialog(theWindow, new JScrollPane(ja),
						"Simulating the Keyboard and Display",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		return help;
	}


	// ////////////////////////////////////////////////////////////////////////////////////
	// Private methods defined to support the above.
	// ////////////////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////////////////////////////////////
	// UI components and layout for upper part of GUI, where simulated display
	// is located.
	private JComponent buildDisplay()
	{
		display = new DisplayPanel();
		display.setPreferredSize(preferredDisplayDimension);
		return display;
	}

	// ////////////////////////////////////////////////////////////////////////////////////
	// UI components and layout for lower part of GUI, where simulated keyboard
	// is located.
	// ////////////////////////////////////////////////////////////////////////////////////
	private JComponent buildKeyboard()
	{
		keyboardPanel = new JPanel(new BorderLayout());
		keyEventAccepter = new JTextField();
		keyEventAccepter.setEditable(true);
		keyEventAccepter.setFont(defaultFont);
		keyEventAccepter.setMargin(textAreaInsets);
		keyEventAccepter.addKeyListener(new KeyboardKeyListener());
		keyboardPanel.add(keyEventAccepter);
		TitledBorder tb = new TitledBorder(
				"KEYBOARD: Characters typed in this text area are written to Receiver Data Register ("
						+ Binary.intToHexString(RECEIVER_DATA) + ")");
		tb.setTitleJustification(TitledBorder.CENTER);
		keyboardPanel.setBorder(tb);
		keyboardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, MAX_KEYBOARD_PANEL_HEIGHT));
		return keyboardPanel;
	}

	// //////////////////////////////////////////////////////////////////
	// update the MMIO Control register memory cell. We will delegate.
	private void updateMMIOControl(int addr, int intValue)
	{
		updateMMIOControlAndData(addr, intValue, 0, 0, true);
	}

	// ///////////////////////////////////////////////////////////////////
	// update the MMIO Control and Data register pair -- 2 memory cells. We will
	// delegate.
	private void updateMMIOControlAndData(int controlAddr, int controlValue,
			int dataAddr, int dataValue)
	{
		updateMMIOControlAndData(controlAddr, controlValue, dataAddr,
				dataValue, false);
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////
	// This one does the work: update the MMIO Control and optionally the Data
	// register as well
	// NOTE: last argument TRUE means update only the MMIO Control register;
	// FALSE means update both Control and Data.
	private synchronized void updateMMIOControlAndData(int controlAddr,
			int controlValue, int dataAddr, int dataValue, boolean controlOnly)
	{
		if (!this.isBeingUsedAsAMarsTool
				|| (this.isBeingUsedAsAMarsTool && connectButton.isConnected()))
		{
			synchronized (Globals.memoryAndRegistersLock)
			{
				try
				{
					Globals.memory.setRawWord(controlAddr, controlValue);
					if (!controlOnly)
						Globals.memory.setRawWord(dataAddr, dataValue);
				} catch (AddressErrorException aee)
				{
					System.out
							.println("Tool author specified incorrect MMIO address!"
									+ aee);
					System.exit(0);
				}
			}
			// HERE'S A HACK!! Want to immediately display the updated memory
			// value in MARS
			// but that code was not written for event-driven update (e.g.
			// Observer) --
			// it was written to poll the memory cells for their values. So we
			// force it to do so.

			if (Globals.getGui() != null
					&& Globals.getGui().getMainPane().getExecutePane()
							.getTextSegmentWindow().getCodeHighlighting())
			{
				Globals.getGui().getMainPane().getExecutePane()
						.getDataSegmentWindow().updateValues();
			}
		}
	}

	// ///////////////////////////////////////////////////////////////////
	// Return value of the given MMIO control register after ready (low order)
	// bit set (to 1).
	// Have to preserve the value of Interrupt Enable bit (bit 1)
	private static boolean isReadyBitSet(int mmioControlRegister)
	{
		try
		{
			return (Globals.memory.get(mmioControlRegister,
					Memory.WORD_LENGTH_BYTES) & 1) == 1;
		} catch (AddressErrorException aee)
		{
			System.out.println("Tool author specified incorrect MMIO address!"
					+ aee);
			System.exit(0);
		}
		return false; // to satisfy the compiler -- this will never happen.
	}

	// ///////////////////////////////////////////////////////////////////
	// Return value of the given MMIO control register after ready (low order)
	// bit set (to 1).
	// Have to preserve the value of Interrupt Enable bit (bit 1)
	private static int readyBitSet(int mmioControlRegister)
	{
		try
		{
			return Globals.memory.get(mmioControlRegister,
					Memory.WORD_LENGTH_BYTES) | 1;
		} catch (AddressErrorException aee)
		{
			System.out.println("Tool author specified incorrect MMIO address!"
					+ aee);
			System.exit(0);
		}
		return 1; // to satisfy the compiler -- this will never happen.
	}

	// ///////////////////////////////////////////////////////////////////
	// Return value of the given MMIO control register after ready (low order)
	// bit cleared (to 0).
	// Have to preserve the value of Interrupt Enable bit (bit 1). Bits 2 and
	// higher don't matter.
	private static int readyBitCleared(int mmioControlRegister)
	{
		try
		{
			return Globals.memory.get(mmioControlRegister,
					Memory.WORD_LENGTH_BYTES) & 2;
		} catch (AddressErrorException aee)
		{
			System.out.println("Tool author specified incorrect MMIO address!"
					+ aee);
			System.exit(0);
		}
		return 0; // to satisfy the compiler -- this will never happen.
	}

	// ///////////////////////////////////////////////////////////////////
	// Transmit delay is simulated by counting instruction executions.
	// Here we simply initialize (or reset) the variables.
	private void initializeTransmitDelaySimulator()
	{
		this.countingInstructions = false;
		this.instructionCount = 0;
		this.transmitDelayInstructionCountLimit = 0;
	}

	private void doDebugCommands()
	{
		/*
		debugDisplayCommand(0x00040000); //0x04 (not a valid command)
		debugDisplayCommand(0x00FF0000); //ENQ RES
		debugDisplayCommand(0x00FF0100); //ENQ FNT
		debugDisplayCommand(0xD1511FF0); //PUT D1 AT 51 1F WITH 0xF0
		debugDisplayCommand(0x0003FFFF); //FNT APL
		debugDisplayCommand(0x00FF0100); //ENQ FNT
		debugDisplayCommand(0x00FF0200); //ENQ CLR
		debugDisplayCommand(0x00012D0E); //CLR 0x2D0E
		debugDisplayCommand(0x00FF0200); //ENQ CLR
		debugDisplayCommand(0x00FF0000); //ENQ RES	
		debugDisplayCommand(0xC15220F0); //PUT D1 AT 52 20 WITH 0xF0
		*/
		
	}
	
	private void debugDisplayCommand(int command)
	{
		try
		{
			DisplayCommandResults r =	display.processCommand(command);

			System.out.println("COMMAND PROCESSED: " + formatCommandInt(command) 
					         + "\nCOMMAND RESULT: " + formatCommandInt(r.getStatus())
					         + "\nIncurred delay: " + r.getDelay()
					         );
		} 
		catch (Exception e)
		{
			System.err.println("Exception occurred when processing command!\n\n");
			e.printStackTrace();
		}
	}
	
	private String formatCommandInt(int theInt)
	{
		String result = Long.toHexString(((long) theInt & 0xFFFFFFFFL) | 0x100000000L).substring(1);
		result = "0x" + result + " (";
		String binaryResult = Integer.toBinaryString(theInt);
		while (binaryResult.length() < 32)
			binaryResult = "0" + binaryResult;
		result += binaryResult + ")";
		return result;
	}
	
	// /////////////////////////////////////////////////////////////////////////////////
	//
	// Class to grab keystrokes going to keyboard echo area and send them to
	// MMIO area
	//

	
	
	private class KeyboardKeyListener implements KeyListener
	{
		public void keyTyped(KeyEvent e)
		{
			keyEventAccepter.setText("");
			handleKeyboardInput(e.getKeyChar() & 0x00000ff);
		}

		/* Ignore key pressed event from the text field. */
		public void keyPressed(KeyEvent e)
		{	
			if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED
				&& e.getKeyCode() != KeyEvent.VK_SHIFT)
			{
				keyEventAccepter.setText("");
				handleKeyboardInput((e.getKeyCode() & 0x000000ff) << 8);
			}
		}

		/* Ignore key released event from the text field. */
		public void keyReleased(KeyEvent e)
		{
		}
		
		public void handleKeyboardInput(int dataFromKeyboard)
		{
			int updatedReceiverControl = readyBitSet(RECEIVER_CONTROL);
			updateMMIOControlAndData(RECEIVER_CONTROL, updatedReceiverControl,
					RECEIVER_DATA, dataFromKeyboard);
			if ( updatedReceiverControl != 1 
					&& (Coprocessor0.getValue(Coprocessor0.STATUS) & 2) == 0 // Added by Carl Hauser, Nov 2008
					&& (Coprocessor0.getValue(Coprocessor0.STATUS) & 1) == 1)
			{
				// interrupt-enabled bit is set in both Receiver Control and in
				// Coprocessor0 Status register, and Interrupt Level Bit is 0,
				// so trigger external interrupt.
				mars.simulator.Simulator.externalInterruptingDevice = Exceptions.EXTERNAL_INTERRUPT_KEYBOARD;
			}			
		}
	}

	private class DisplayPanel extends JPanel
	{
		static final int MIN_HORIZ_RES_CHARS = 40;
		static final int MIN_VERT_RES_CHARS = 25;
		static final int MAX_HORIZ_RES_CHARS = 255;
		static final int MAX_VERT_RES_CHARS = 128;

		static final String IBM_FONT_FILENAME = "perfectdos.ttf";
		static final int IBM_FONT_WIDTH = 8;
		static final int IBM_FONT_HEIGHT = 15;
		static final float IBM_FONT_SIZE_OFFSET = 1.0f;
		
		static final String APPLE_FONT_FILENAME = "PrintChar21.ttf";
		static final int APPLE_FONT_WIDTH = 7;
		static final int APPLE_FONT_HEIGHT = 8;
		static final float APPLE_FONT_SIZE_OFFSET = 0.0f;
		
		int charOffsetY, charOffsetX, charBGOffsetY;

		static final int COLOR_MODE = BufferedImage.TYPE_USHORT_565_RGB;

		Font ibmFont, appleFont, currentFont;
		boolean currentFontIsUnicode;

		BufferedImage screen;
		Graphics2D imageWriter;

		int currentFontWidth;
		int currentFontHeight;
		int screenWidth = 0, screenHeight = 0;

		int reg0, reg1, reg2, reg3;

		Container myWindow;
		int hSizeDelta, vSizeDelta;
				
		public DisplayPanel()
		{
			super();
			try
			{
				ibmFont = Font.createFont(Font.TRUETYPE_FONT, 
						this.getClass().getResourceAsStream(IBM_FONT_FILENAME))
						.deriveFont(IBM_FONT_HEIGHT + IBM_FONT_SIZE_OFFSET);
				appleFont = Font.createFont(Font.TRUETYPE_FONT,
						this.getClass().getResourceAsStream(APPLE_FONT_FILENAME))
						.deriveFont(APPLE_FONT_HEIGHT + APPLE_FONT_SIZE_OFFSET);
			} catch (Exception e)
			{
				System.err.println("Exception in loading fonts from file!");
				e.printStackTrace();
			}

			setIBMFont();
		}
		
		private void setIBMFont()
		{
			this.currentFont = ibmFont;
			this.currentFontWidth = IBM_FONT_WIDTH;
			this.currentFontHeight = IBM_FONT_HEIGHT;
			this.currentFontIsUnicode = false;
			this.charOffsetX = 0;
			this.charOffsetY = 15;
			this.charBGOffsetY = 3;
		}
		
		private void setAppleFont()
		{
			this.currentFont = appleFont;
			this.currentFontWidth = APPLE_FONT_WIDTH;
			this.currentFontHeight = APPLE_FONT_HEIGHT;
			this.currentFontIsUnicode = true;
			this.charOffsetX = 0;
			this.charOffsetY = 7;
			this.charBGOffsetY = 0;
		}

		public void reinit()
		{
			if (this.getSize().width > 0 && this.getSize().height > 0)
			screen = new BufferedImage(this.getSize().width,
					this.getSize().height, COLOR_MODE);
			else
			{
				screen = new BufferedImage(800, 800, COLOR_MODE);
			}
			imageWriter = screen.createGraphics();
			imageWriter.setFont(currentFont);
		}

		public DisplayCommandResults processCommand(int command)
		{
			int status = 0;
			int delay = 0;

			byte r3 = (byte) command;
			byte r2 = (byte) (command >> 8);
			byte r1 = (byte) (command >> 16);
			byte r0 = (byte) (command >> 24);
			
			/*
			 * Widening of the four bytes to ints is done to get rid of the sign.
			 * Eg. r0 = 0xFF; reg0 = 255 dec.
			 */

			reg0 = r0 < 0 ? 256 + r0 : r0;
			reg1 = r1 < 0 ? 256 + r1 : r1;
			reg2 = r2 < 0 ? 256 + r2 : r2;
			reg3 = r3 < 0 ? 256 + r3 : r3;
			
			if (reg0 != 0 && reg0 != 0xFF)
			{
				int charX = (reg1 * currentFontWidth) + 0;
				int charY = (reg2 * currentFontHeight) + 0;
				
				if (charX >= this.getWidth() || charY > this.getHeight())
				{
					status |= DisplayCommandResults.OFFSCREEN_CHAR;
				}

				if (resolveBGColor(reg3).equals(resolveFGColor(reg3)))
				{
					status |= DisplayCommandResults.INVISIBLE_CHAR;
				}
				
				imageWriter.setColor(resolveBGColor(reg3));
				imageWriter.fillRect(charX, charY + charBGOffsetY, currentFontWidth,
						currentFontHeight);

				imageWriter.setColor(resolveFGColor(reg3));
				
				char[] theChar = new char[1];
				
				if (!currentFontIsUnicode)
				{
					theChar[0] = (char) reg0;
				}
				else
				{	 
					theChar[0] = convertByte(r0);
				}
				
				imageWriter.drawChars(theChar, 0, 1, charX + charOffsetX, charY + charOffsetY);	
				repaint();
				return new DisplayCommandResults(status, 0);
			} 
			else
			{
				int newHSize, newVSize;
				switch (reg1)
				{
				case 0:
					/*
					 * NOP: do nothing
					 */
					return new DisplayCommandResults(status, 0);
				case 1:
					/*
					 * CLR: clear screen with color in r2, r3
					 */
					imageWriter.setBackground(resolveWideColor(reg2, reg3));
					imageWriter.clearRect(0, 0, this.getWidth(), this.getHeight());
					delay = 125;
					repaint();
					break;
				case 2:
					/*
					 * RES: resize screen
					 */
					if (reg2 < MIN_HORIZ_RES_CHARS
					 || reg2 > MAX_HORIZ_RES_CHARS
					 || reg3 < MIN_VERT_RES_CHARS
					 || reg3 > MAX_VERT_RES_CHARS)
					{
						return new DisplayCommandResults(status | DisplayCommandResults.INVALID_ARGS, 0);
					}
					screenWidth = newHSize = (reg2 * currentFontWidth);
					screenHeight = newVSize = (reg3 * currentFontHeight);
					/*
					 * When the display panel grows, so too must the window containing it.
					 * The amount the window will grow (or shrink) is the same amount as the display
					 * panel, regardless of the original size of the window.
					 */
					hSizeDelta = newHSize - this.getWidth();
					vSizeDelta = newVSize - this.getHeight();
					
					this.setSize(newHSize, newVSize);
										
					myWindow = this.getRootPane().getParent();

					/*
					 * Changing the window size, making a new image, and repainting
					 * takes a long time, and it all MUST happen before another command
					 * is processed. We can't rely on the delay alone--different computers
					 * run at different speeds. So, we must wait for the EDT.
					 */
					repaintWithDelay();
					delay = 7500;
					break;
				case 3:
					/*
					 * FNT: change font
					 * Note! The H in newHSize = HORIZONTAL = WIDTH! (Not height!)
					 */
					screenHeight = newVSize = (this.getHeight() / currentFontHeight);
					screenWidth = newHSize = (this.getWidth() / currentFontWidth);
					
					if (reg2 == 0)
						setIBMFont();
					else
						setAppleFont();
					
					newHSize *= currentFontWidth;
					newVSize *= currentFontHeight;
					
					/*
					 * When the display panel grows, so too must the window containing it.
					 * The amount the window will grow (or shrink) is the same amount as the display
					 * panel, regardless of the original size of the window.
					 */
					hSizeDelta = newHSize - this.getWidth();
					vSizeDelta = newVSize - this.getHeight();
					
					this.setSize(newHSize, newVSize);
										
					myWindow = this.getRootPane().getParent();

					/*
					 * Changing the window size, making a new image, and repainting
					 * takes a long time, and it all MUST happen before another command
					 * is processed. We can't rely on the delay alone--different computers
					 * run at different speeds. So, we must wait for the EDT.
					 */
					repaintWithDelay();
					delay = 7500;
					break;
				
				case 255:
					/*
					 * ENQ: enquire
					 */
					DisplayCommandResults results = new DisplayCommandResults(status, 0);
					switch (reg2)
					{
					case 0:
						/*
						 * this.getHeight() / currentFontHeight, etc.
						 * does not always give the correct character resolution after changing font size.
						 * To scoot around this problem, this.getHeight() is used only if the RES command has 
						 * ...
						 */
						results.setEnqLo(screenHeight == 0 ? preferredDisplayDimension.height / currentFontHeight
								: screenHeight / currentFontHeight);
						results.setEnqHi(screenWidth == 0 ? preferredDisplayDimension.width / currentFontWidth
								: screenWidth / currentFontWidth);
						break;
					case 1:
						results.setEnqWide(currentFont == ibmFont ? 0 : 1);
						break;
					case 2:
						results.setEnqWide(resolveColorWide(imageWriter.getBackground() != null ? imageWriter.getBackground() : Color.BLACK));
					}
					return results;
				default:
					// Do nothing.
					return new DisplayCommandResults(status | DisplayCommandResults.INVALID_CMD, 0);
				}
				return new DisplayCommandResults(status, delay);
			}
		}
				
		public void repaintWithDelay()
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						myWindow.setSize(new Dimension(
								myWindow.getWidth() + hSizeDelta,
								myWindow.getHeight() + vSizeDelta));
						reinit();
						repaint();
					}
				});
			} catch (Exception ex)
			{
				ex.printStackTrace();
			} 			
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

		public char convertByte(byte theByte)
		{
			CharBuffer result;
			byte[] theBytes = new byte[1];
			theBytes[0] = theByte;
			
			CharsetDecoder c = Charset.forName("cp437").newDecoder();
			try
			{
				result = c.decode(ByteBuffer.wrap(theBytes));
			} catch (Exception e)
			{
				result = null;
				e.printStackTrace();
			}
			return result.get();
		}
		
		public Color resolveBGColor(int theColor)
		{
			// Color format:
			// 0000 0000
			// FG BG
			// First bit: intense on/off
			// Bits 1-3: R, G, B
			theColor &= 0x000000F;
			int intensity = ((theColor >> 3) * 128) + 127;
			int r = (theColor >> 2 & 1) * intensity;
			int g = (theColor >> 1 & 1) * intensity;
			int b = (theColor & 1) * intensity;
			return new Color(r, g, b);
		}

		public Color resolveFGColor(int theColor)
		{
			// Color format:
			// 0000 0000
			//  FG   BG
			// First bit: intense on/off
			// Bits 1-3: R, G, B
			theColor = (theColor & 0x000000F0) >> 4;
			int intensity = ((theColor >> 3) * 128) + 127;
			int r = (theColor >> 2 & 1) * intensity;
			int g = (theColor >> 1 & 1) * intensity;
			int b = (theColor & 1) * intensity;
			return new Color(r, g, b);
		}
		
		public Color resolveWideColor(int hi, int lo)
		{
			int color = (hi << 8) + lo;
			float r = (float)((color & 0xF800) >> 11) / 31.0f;
			float g = (float)((color & 0x07E0) >> 5) / 63.0f;
			float b = (float)((color & 0x001F)) / 31.0f;
			return new Color(r, g, b);
		}
		
		/*
		 * Does the opposite of resolveWideColor: takes a Java Color
		 * and converts it to a 16-bit (5-6-5, RGB) integer.
		 * Used by ENQ when querying the background color.
		 */
		public int resolveColorWide(Color theColor)
		{
			int color = 0;
			float[] vals = theColor.getColorComponents(new float[3]);
			color += (int)(vals[0] * 31.0f) << 11;
			color += (int)(vals[1] * 63.0f) << 5;
			color += (int)(vals[2] * 31.0f);
			return color;
		}

	}

	private class DisplayCommandResults
	{
		public static final int READY = 		 0b00000001;
		public static final int OFFSCREEN_CHAR = 0b00010000;
		public static final int INVISIBLE_CHAR = 0b00100000;
		public static final int INVALID_CMD = 	 0b01000000;
		public static final int INVALID_ARGS = 	 0b10000000;
		int status, delay;
		DisplayCommandResults(int s, int d)
		{
			status = s;
			delay = d;
		}
		public void setEnqWide(int newEnq)
		{
			status = (status & 0x0000FFFF) | (newEnq << 16);
		}
		public void setEnqLo(int lo)
		{
			status = (status & 0xFF00FFFF) | (lo << 16);
		}
		public void setEnqHi(int hi)
		{
			status = (status & 0x00FFFFFF) | (hi << 24);
		}
		public int getStatus()
		{
			if (delay == 0)
				return status | READY;
			return status;
		}
		public int getDelay()
		{
			return delay;
		}
	}
}