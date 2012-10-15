/**
 * Master Time Keeper – The perfect graphical terminal schedule viewer
 * 
 * Copyright © 2012  Mattias Andrée (maandree@kth.se)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.kth.maandree.mastertimekeeper;

import java.io.*;
import java.util.*;


/**
 * This is the main class of the program
 * 
 * @author  Mattias Andrée, <a href="mailto:maandree@kth.se">maandree@kth.se</a>
 */
public class Program
{
    /**
     * Hidden constructor
     */
    private Program()
    {
	//Nullify default constructor
    }
    
    
    
    /**
     * This is the main entry point of the program
     *
     * @param   args       Start up arguments (unused)
     * @throws  Throwable  On any error
     */
    public static void main(final String... args) throws Throwable
    {
	colourmap.put("!", "31");
	colourmap.put("*", "33;1");
	colourmap.put("~", "35;1");
	colourmap.put("^", "36");
	colourmap.put("-", "35");
	colourmap.put("+", "31");
	colourmap.put("/", "32;1");
	colourmap.put("?", "33");
	colourmap.put("&", "34");
	colourmap.put("#", "32");
	colourmap.put(">", "34;1");
	
	final Calendar now = Calendar.getInstance();
	final int day   = now.get(Calendar.DAY_OF_MONTH);
	final int month = now.get(Calendar.MONTH);
	final int year  = now.get(Calendar.YEAR);
	final int week  = now.get(Calendar.WEEK_OF_YEAR);
	
	try
	{
	    final ArrayList<String> lines = new ArrayList<String>();
	    final ArrayList<String> raws = new ArrayList<String>();
	    final Scanner fileScanner = new Scanner(new BufferedInputStream(new FileInputStream(new File(args[0]))));
	    int cur = 0, lineno = 0;
	    while (fileScanner.hasNext())
	    {
		final String line;
		lines.add(manipulateLine(line = fileScanner.nextLine(), day, month, year, week));
		raws.add(line);
		if (correctDay)
		{
		    cur = lineno;
		    correctDay = false;
		}
		lineno++;
	    }
	    
	    Terminal.initialiseTerminal();
	    Terminal.setEchoFlag(false);
	    Terminal.setBufferFlag(false);
	    Terminal.setSignalFlag(false);
	    Terminal.setCursorVisibility(false);
	    
	    StringBuilder buf = new StringBuilder("\033[H\033[2J");
	    
	    int width  = Terminal.getTerminalWidth();
	    int height = Terminal.getTerminalHeight();
	    int bottom = 2;
	    int top = 0;
	    while ((top < raws.size()) && (raws.get(top).startsWith("--- ") == false))
		top++;
	    if (top >= raws.size())
		top = 0;
	    int dispheight = height - bottom - top;
	    if (cur < top)
		cur = top;
	    
	    for (int i = 0; i < top; i++)
		buf.append(lines.get(i));
	    for (int i = cur, lim = cur + height - bottom - top; i < lim; i++)
		buf.append(i < lines.size() ? lines.get(i) : "\033[2m~\033[22m\n");
	    String procent = (lines.size() < height - bottom) ? "ALL" : "TOP";
	    buf.append("\033[44;33;1m\033[2K  " + procent + "  \033[49;39;21m\n");
	    System.out.print(buf.toString());
	    buf = new StringBuilder();
	    
	    int lastWidth = width, lastHeight = height;
	    
	    for (int d = 0; d >= 0; d = System.in.read())
	    {
		if (d == 'q')
		    break;
		
		width  = Terminal.getTerminalWidth();
		height = Terminal.getTerminalHeight();
	        dispheight = height - bottom - top;
		boolean resized = (width != lastWidth) || (height != lastHeight);
		
		int last = cur;
		switch (d)
		{
		    case 53: //page up
			cur -= dispheight;
			break;
		    case 65: //up
			cur--;
			break;
			
		    case 54: //page down
		    case 32: //space
			cur += dispheight;
			break;
		    case 66: //down
		    case 10: //enter
			cur++;
			break;
		}
		if (cur > lines.size() - dispheight / 4)
		    cur = lines.size() - dispheight / 4;
		if (cur < top)
		    cur = top;
		
		int diff = cur - last;
		if ((diff == 0) && (resized == false))
		    continue;
		
		if ((diff == 1) && (resized == false))
		{
		    buf.append("\033[" + (height - 1) + ";1H\033[2K\033[1S\033[1;1H");
		    for (int i = 0; i < top; i++)
			buf.append("\033[2K" + lines.get(i));
		    buf.append("\033[" + (height - 2) + ";1H");
		    int i = last + dispheight;
		    buf.append("\033[2K" + (i < lines.size() ? lines.get(i) : "\033[2m~\033[22m\n"));
		}
		else if ((diff == -1) && (resized == false))
		{
		    buf.append("\033[" + (height - 1) + ";1H\033[2K\033[1T\033[1;1H");
		    for (int i = 0; i < top; i++)
			buf.append("\033[2K" + lines.get(i));
		    buf.append("\033[2K" + (cur < lines.size() ? lines.get(cur) : "\033[2m~\033[22m\n"));
		    buf.append("\033[" + (height - 1) + ";1H");
		}
		else
		{
		    buf.append("\033[1;1H\033[2J");
		    for (int i = 0; i < top; i++)
			buf.append(lines.get(i));
		    for (int i = cur, lim = cur + dispheight; i < lim; i++)
			buf.append(i < lines.size() ? lines.get(i) : "\033[2m~\033[22m\n");
		}
		
		procent = String.valueOf((int)((cur - top) * 100. / (lines.size() - top - dispheight + 1) + 0.5));
		if (procent.length() == 1)
		    procent = '0' + procent;
		if (lines.size() < height - bottom)               procent = "ALL";
		else if (cur == top)                              procent = "TOP";
		else if (cur == lines.size() - dispheight + 1)    procent = "BOT";
		else
		    procent += '%';
		buf.append("\033[44;33;1m\033[2K  " + procent + "  \033[49;39;21m\n");
		
		System.out.print(buf.toString());
		buf = new StringBuilder();
		
		lastWidth = width;
		lastHeight = height;
	    }
	}
	catch (final Throwable err)
	{
	    System.err.println("\033[31mFatal exception:\033[m\n");
	    throw err;
	}
	finally
        {
	    Terminal.setEchoFlag(true);
	    Terminal.setBufferFlag(true);
	    Terminal.setSignalFlag(true);
	    Terminal.setCursorVisibility(true);
	    Terminal.terminateTerminal();
	}
    }
    
    
    
    /**
     * Used by {@link #manipulateLine(String)}
     */
    private static int legendState = 0;
    
    /**
     * Used by {@link #manipulateLine(String)}
     */
    private static final HashMap<String, String> colourmap = new HashMap<String, String>();
    
    /**
     * Used by {@link #manipulateLine(String)}
     */
    private static boolean correctYear = false;
    
    /**
     * Used by both methods
     */
    private static boolean correctDay = false;
    
    
    
    /**
     * Adds colours to a line
     *
     * @param   line   The line
     * @param   day    The current day of the month
     * @param   month  The current month of the year
     * @param   year   The current year
     * @param   week   The current week of the year
     * @return         The line colourised
     */
    public static String manipulateLine(final String line, final int day, final int month, final int year, final int week)
    {
	final StringBuilder out = new StringBuilder();
	
	if (line.startsWith("--- "))
	{
	    if (line.startsWith("--- Legend ---"))
		legendState = 1;
	    else if (legendState == 1)
		legendState = 2;
	    out.append("\033[47;30m\033[2K");
	    out.append(line);
	    out.append("\033[49;39m\n");
	}
	else if (legendState < 2)
	{
	    final String colour;
	    if ((legendState == 1) && (line.length() > 0) && ((colour = colourmap.get(line.substring(0, 1))) != null))
	    {
		out.append("\033[" + colour + "m");
		out.append(line.substring(0, 1));
		out.append("\033[21;39;49;0m");
		out.append(line.substring(1));
	    }
	    else
		out.append(line);
	    out.append('\n');
	}
	else if (line.startsWith(">>"))
	{
	    out.append("\033[31m");
	    out.append(line);
	    out.append("\033[39m\n");
	}
	else if (line.startsWith("::"))
	{
	    correctDay = correctYear = line.startsWith("::Vecka " + week + ", " + year);
	    if (correctYear)
		out.append("\033[32;1m");
	    else
		out.append("\033[34;1m");
	    out.append(line.substring(0, 2));
	    out.append("\033[21m");
	    out.append(line.substring(2));
	    out.append("\033[39m\n");
	}
	else if (line.startsWith("#"))
	{
	    out.append("\033[32m");
	    out.append(line);
	    out.append("\033[39m\n");
	}
	else if (line.length() > 53)
	{
	    if (correctYear && line.substring(6, 8).equals((month < 9 ? "0" : "") + (month + 1)) && line.substring(13, 15).equals((day < 10 ? "0" : "") + day))
	    {
		correctDay = true;
		out.append("\033[1;32m");
		out.append(line.substring(0, 16));
		out.append("\033[21;39m");
	    }
	    else
		out.append(line.substring(0, 16));
	    String colour;
	    if ((colour = colourmap.get(line.substring(16, 17))) != null)
	    {
		out.append("\033[" + colour + "m");
		out.append(line.substring(16, 17));
		out.append("\033[21;39;49;0m");
		if ((colour = colourmap.get(line.substring(29, 30))) != null)
		{
		    out.append(line.substring(17, 29));
		    out.append("\033[" + colour + "m");
		    out.append(line.substring(29, 30));
		    out.append("\033[21;39;49;0m");
		}
		else
		    out.append(line.substring(17, 30));
	    }
	    else if ((colour = colourmap.get(line.substring(29, 30))) != null)
	    {
		out.append(line.substring(16, 29));
		out.append("\033[" + colour + "m");
		out.append(line.substring(29, 30));
		out.append("\033[21;39;49;0m");
	    }
	    else
		out.append(line.substring(16, 30));
	    
	    if (line.substring(30, 41).toLowerCase().equals(line.substring(30, 41).toUpperCase()))
		out.append("\033[32m");
	    else if (line.substring(30, 41).equals(line.substring(30, 41).toUpperCase()))
		out.append("\033[31m");
	    else if (line.substring(30, 41).equals(line.substring(30, 41).toLowerCase()))
		out.append("\033[33m");
	    
	    out.append(line.substring(30, 43));
	    out.append("\033[39m");
	    if (line.substring(43, 51).startsWith("<"))
		out.append("\033[31m");
	    out.append(line.substring(43, 53));
	    out.append("\033[39m");
	    if (line.charAt(53) == '?')
	        out.append("\033[31m?\033[39m");
	    else
		out.append(line.charAt(53));
	    out.append(line.substring(54));
	    out.append('\n');
	}
	else
	{
	    out.append(line);
	    out.append('\n');
	}
	return out.toString();
    }
    
}

