
/**
* AudioManipulation.java
*
* Time-stamp: <2019-02-12 08:57:09 rlc3>
*
* Defines mixer/effect functions on audio streams
* Utilises the AudioInputStream class 
* 
* To compile: javac -classpath editor.jar:. RunEffects.java
* To run use: java -classpath editor.jar:. RunEffects
* 
*/

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;

public class AudioManipulation
{

	/**** echo *****************************************************************/

	public static AudioInputStream echo(AudioInputStream ais, int timeDelay, double fading0, double fading1)
	{

		byte[] a = null;
		int[] data, ch0, ch1;
		int max;

		try
		{

			// AudioInputStream methods
			int numChannels = ais.getFormat().getChannels();
			int sampleSize = ais.getFormat().getSampleSizeInBits();
			boolean isBigEndian = ais.getFormat().isBigEndian();
			float sampleRate = ais.getFormat().getSampleRate();
			float frameRate = ais.getFormat().getFrameRate();
			int frameSize = ais.getFormat().getFrameSize();
			int frameLength = (int) ais.getFrameLength();

			// sampleRate = framerate = 44100.0 Hz (playback rate = sampling rate!)
			// 1 sec = 1000 millisecs
			// calculate delay in frames
			int frameDelay = (int) (timeDelay / 1000.0 * frameRate);

			// reset the AudioInputStream (mark goes to the start)
			ais.reset();

			// create a byte array of the right size
			// recall the lecture OHP slides ..
			a = new byte[(int) frameLength * frameSize];

			// fill the byte array with the data of the AudioInputStream
			ais.read(a);

			// Create an integer array, data, of the right size
			// only reason to do this is enabling type double mixing calculations
			// Each (channel) sample is made of 2 = sampleSize/8 bytes
			data = new int[a.length / 2];

			// fill the integer array by combining two 2 = sampleSize/8 bytes per sample of
			// the
			// byte array a into one integer
			// Bytes HB and LB Big Endian make up one integer
			for (int i = 0; i < data.length; ++i)
			{
				/* First byte is HB (most significant digits) - coerce to 32-bit int */
				// HB =def sign_extend(a[2*i]) from 8 bit byte to 32 bit int
				int HB = (int) a[2 * i];
				/* Second byte is LB (least significant digits) - coerce to 32-bit int */
				// LB =def sign_extend(a[2*i+1]) from 8 bit byte to 32 bit int
				int LB = (int) a[2 * i + 1];
				// note that data[i] =def sign_extend(HB.LB)
				// | : Bool^32 x Bool^32 -----> Bool^32 where Bool = {0, 1}
				data[i] = HB << 8 | (LB & 0xff);
			}

			// split integer data array samples into two channels
			// if both channels are faded by the same factor
			// then there is no need to split the channels
			ch0 = new int[data.length / 2];
			ch1 = new int[data.length / 2];
			for (int i = 0; i < data.length / 2; i++)
			{
				ch0[i] = data[2 * i];
				ch1[i] = data[2 * i + 1];
			}

			// Adding a faded copy of the early signal to the later signal
			// THIS IS THE ECHO !!
			for (int i = frameDelay; i < ch0.length; ++i)
			{
				ch0[i] += (int) (ch0[i - frameDelay] * fading0);
				ch1[i] += (int) (ch1[i - frameDelay] * fading1);
			}

			// combine the two channels
			for (int i = 0; i < data.length; i += 2)
			{
				data[i] = ch0[i / 2];
				data[i + 1] = ch1[i / 2];
			}

			// get the maximum amplitute
			max = 0;
			for (int i = 0; i < data.length; ++i)
			{
				max = Math.max(max, Math.abs(data[i]));
			}

			// 16 digit 2s-complement range from -2^15 to +2^15-1 = 256*128-1
			// therefore we linearly scale data[i] values to lie within this range ..
			// .. so that each data[i] has a 16 digit "HB.LB binary representation"
			if (max > 256 * 128 - 1)
			{
				System.out.println("Sound values are linearly scaled by " + (256.0 * 128.0 - 1) / max
						+ " because maximum amplitude is larger than upper boundary of allowed value range.");
				for (int i = 0; i < data.length; ++i)
				{
					data[i] = (int) (data[i] * (256.0 * 128.0 - 1) / max);
				}
			}

			// convert the integer array to a byte array
			for (int i = 0; i < data.length; ++i)
			{
				a[2 * i] = (byte) ((data[i] >> 8) & 0xff);
				a[2 * i + 1] = (byte) (data[i] & 0xff);
			}

		} catch (Exception e)
		{
			System.out.println("Something went wrong");
			e.printStackTrace();
		}

		// create a new AudioInputStream out of the the byteArray
		// and return it.
		return new AudioInputStream(new ByteArrayInputStream(a), ais.getFormat(), ais.getFrameLength());
	}

	/****
	 * scaleToZero
	 *****************************************************************/

	public static AudioInputStream scaleToZero(AudioInputStream ais)
	{

		byte[] a = null;
		int[] data;

		// Decelerations left in to stay close to original in case I need to reconsider
		// some functions
		// It's not necessary to split the channels as they are both scaled the same
		// based on the frame index
		int[] ch0, ch1;

		int max;

		/*
		 * ----- template code commented out BEGIN
		 */
		try
		{

			int frameSize = ais.getFormat().getFrameSize();
			int frameLength = (int) ais.getFrameLength();

			// reset the AudioInputStream (mark goes to the start) ??
			ais.reset();

			// create a byte array of the right size
			// recall the lecture OHP slides ..
			// ??
			a = new byte[(int) frameLength * frameSize];

			// fill the byte array with the data of the AudioInputStream ??
			ais.read(a);

			// Create an integer array, data, of the right size
			// only reason to do this
			// is enabling type float/double mixing calculations ??
			data = new int[a.length / 2];

			// fill the integer array by combining two bytes of the
			// byte array a into
			// one integer - see lectures ??
			for (int i = 0; i < data.length; i++)
			{
				// same as echo
				int HB = (int) a[2 * i];

				int LB = (int) a[2 * i + 1];

				data[i] = HB << 8 | (LB & 0xff);
			}

			// scale data linearly by a factor of 3/4
			// **** NB this is the only part of
			// scaleToZero that is not already part of
			// echo effect !!!! **** ??
			for (int i = 0; i < data.length / 2; i++)
			{
				// on the last frame of the audio this should evaluate to 1-(3/4) => 1/4
				double factor = 1 - ((3.0f / 4.0f) * (double) i / (double) (data.length / 2 - 1));
				// if (i % 1000 == 0) System.out.println("scaling frame " + i + " with a factor
				// of " + factor);
				data[2 * i] = (int) ((double) data[2 * i] * factor);
				data[2 * i + 1] = (int) ((double) data[2 * i + 1] * factor);
			}

			// *** This block is likely redundant as we only ever scale the amplitude down

			// get the maximum amplitude
			max = 0;
			for (int i = 0; i < data.length; ++i)
			{
				max = Math.max(max, Math.abs(data[i]));
			}

			// 16 digit 2s-complement range from -2^15 to +2^15-1 = 256*128-1
			// therefore we linearly scale data[i] values to lie within this range ..
			// .. so that each data[i] has a 16 digit "HB.LB binary representation"
			if (max > 256 * 128 - 1)
			{
				System.out.println("Sound values are linearly scaled by " + (256.0 * 128.0 - 1) / max
						+ " because maximum amplitude is larger than upper boundary of allowed value range.");
				for (int i = 0; i < data.length; ++i)
				{
					data[i] = (int) (data[i] * (256.0 * 128.0 - 1) / max);
				}
			}

			// ****

			// convert the integer array to a byte array
			for (int i = 0; i < data.length; ++i)
			{
				a[2 * i] = (byte) ((data[i] >> 8) & 0xff);
				a[2 * i + 1] = (byte) (data[i] & 0xff);
			}

		} catch (Exception e)
		{
			System.out.println("Something went wrong");
			e.printStackTrace();
		}
		/*
		 * ----- template code commented out END
		 */

		// create a new AudioInputStream out of the the byteArray
		// and return it.
		return new AudioInputStream(new ByteArrayInputStream(a), ais.getFormat(), ais.getFrameLength());
	}

	/**** addNote *****************************************************************/

	public static AudioInputStream addNote(AudioInputStream ais, double frequency, int noteLengthInMilliseconds)
	{
		byte[] a = null;
		int[] data;

		/*
		 * ----- template code commented out BEGIN
		 */

		try
		{

			// number of frames for the note of noteLengthInMilliseconds
			int frameSize = ais.getFormat().getFrameSize();
			int numChannels = ais.getFormat().getChannels();
			float frameRate = ais.getFormat().getFrameRate();
			int noteLengthInFrames = (int) ((float) noteLengthInMilliseconds / 1000.0f * frameRate);
			int noteLengthInBytes = noteLengthInFrames * frameSize;
			int noteLengthInInts = noteLengthInBytes / 2;

			a = new byte[noteLengthInBytes];
			data = new int[noteLengthInInts];

			// create the note as a data array of integer samples
			// each sample value data[i] is calculated using
			// the time t at which data[i] is played

			for (int i = 0; i < noteLengthInInts; i += 2)
			{
				// what is the time to play one frame?
				// BEFORE "frame" data[i]data[i+1] plays, how many frames are there?
				// hence compute t in terms of i
				double frameTime = 1 / frameRate;
				double doneFrames = i * 2 / frameSize;
				double t = frameTime * doneFrames;

				//my computer has very customised sound settings which makes
				//this very loud but sticking with the given amplitude of 64*256
				double amplitude = 64*256;

				data[i] = (int) (amplitude * Math.sin(frequency * 2 * Math.PI * t));
				data[i + 1] = (int) (amplitude * Math.sin(frequency * 2 * Math.PI * t));

			}

			// copy the int data[i] array into byte a[i] array ??
			for (int i = 0; i < data.length; i++)
			{
				a[2 * i] = (byte) ((data[i] >> 8) & 0xff);
				a[2 * i + 1] = (byte) (data[i] & 0xff);
			}

		} catch (Exception e)
		{
			System.out.println("Something went wrong");
			e.printStackTrace();
		}
		/*
		 * ----- template code commented out END
		 */

		return append(new AudioInputStream(new ByteArrayInputStream(a), ais.getFormat(),
				a.length / ais.getFormat().getFrameSize()), ais);

	} // end addNote

	/**** append *****************************************************************/

	// THIS METHOD append IS SUPPLIED FOR YOU
	public static AudioInputStream append(AudioInputStream ais1, AudioInputStream ais2)
	{

		byte[] a, b, c = null;
		try
		{
			a = new byte[(int) ais1.getFrameLength() * ais1.getFormat().getFrameSize()];

			// fill the byte array with the data of the AudioInputStream
			ais1.read(a);
			b = new byte[(int) ais2.getFrameLength() * ais2.getFormat().getFrameSize()];

			// fill the byte array with the data of the AudioInputStream
			ais2.read(b);

			c = new byte[a.length + b.length];
			for (int i = 0; i < c.length; i++)
			{
				if (i < a.length)
				{
					c[i] = a[i];
				} else
					c[i] = b[i - a.length];
			}

		} catch (Exception e)
		{
			System.out.println("Something went wrong");
			e.printStackTrace();
		}

		return new AudioInputStream(new ByteArrayInputStream(c), ais1.getFormat(),
				c.length / ais1.getFormat().getFrameSize());
	} // end append

	/**** tune *****************************************************************/

	public static AudioInputStream tune(AudioInputStream ais)
	{
		AudioInputStream temp = null;

		/*
		 * ----- template code commented out BEGIN
		 */

		// create an empty AudioInputStream (of frame size 0)
		// call it temp (already declared above)
		byte[] c = new byte[1];
		temp = new AudioInputStream(new ByteArrayInputStream(c), ais.getFormat(), 0);

		// specify variable names for both the frequencies in Hz and note lengths in
		// seconds
		// eg double C4, D4 etc for frequencies and s, l, ll, lll for lengths
		// Hint: Each octave results in a doubling of frequency.

		double C4 = 261.63;
		double E4 = 329.63;
		double F4 = 349.23;
		double G4 = 392.00;

		double A4 = 440.00;
		double B4 = 493.88;
		double C5 = 523.25;
		double D5 = 587.33;
		double E5 = 2 * E4;
		double Eb5 = 622.25;
		double F5 = 2 * F4;
		double G5 = 2 * G4;

		double A5 = 2 * A4;
		double B5 = 2 * B4;
		double C6 = 2 * C5;
		double D6 = 2 * D5;
		double E6 = 2 * E5;
		double F6 = 2 * F5;
		double G6 = 2 * G5;

		double A6 = 2 * A5;
		double B6 = 2 * B5;
		double C7 = 2 * C6;

		// and the lengths in milliseconds
		int s = 500;
		int l = 2000;
		int ll = 2500;
		int lll = 2800;

		// also sprach zarathustra: 2001 A Space Odyssey
		// specify the tune
		double[][] notes = { { C4, l }, { G4, l }, { C5, l }, { E5, s }, { Eb5, lll }, { C4, l }, { G4, l }, { C5, l },
				{ Eb5, s }, { E5, lll }, { A5, s }, { B5, s }, { C6, l }, { A5, s }, { B5, s }, { C6, l }, { D6, ll },
				{ E6, s }, { F6, s }, { G6, l }, { E6, s }, { F6, s }, { G6, l }, { A6, l }, { B6, l }, { C7, lll } };
		// 100ms between each note EXCEPT: (5 => 6) and (10 => 11) which have 500ms
		// between

		// use addNote to build the tune as an AudioInputStream
		// starting from the empty AudioInputStream temp (above) and adding each note
		// one by one using A LOOP
		// ??

		// start with the last note as addNote places the note at the beginning, NOT the
		// end.
		for (int i = notes.length - 1; i >= 0; i--)
		{
			temp = addNote(temp, notes[i][0], (int) notes[i][1]);
			if (i == 5 || i == 10)
				temp = addNote(temp, 0, 500);
			else
				temp = addNote(temp, 0, 100);
		}

		/*
		 * ----- template code commented out END
		 */

		// append temp, ie the tune, to current ais
		return append(temp, ais);
	}

	/****
	 * altChannels
	 *****************************************************************/

	public static AudioInputStream altChannels(AudioInputStream ais, double timeInterval)
	{

		// plays the ais only in channel 0 for timeInterval, then in channel 1,
		// inverting every [timeInterval] seconds

		int frameSize = ais.getFormat().getFrameSize(); // = 4
		float frameRate = ais.getFormat().getFrameRate();
		int inputLengthInFrames = (int) ais.getFrameLength();
		int frameInterval = (int) (frameRate * timeInterval); // number of frames played during timeInterval
		int inputLengthInBytes = frameSize * inputLengthInFrames;
		int numChannels = ais.getFormat().getChannels(); // = 2

		// byte arrays for input channels and output channels
		int[] ich0, ich1, och0, och1, data;
		byte[] a = null, b = null;

		try
		{

			/*
			 * ----- template code commented out BEGIN
			 */
			// create new byte arrays a for input and b for output of the right size
			a = new byte[(int) inputLengthInBytes];

			// output size increased by factor of numChannels as we play each channels
			// inputs in isolation in sequence
			b = new byte[(int) inputLengthInBytes * 2];

			// fill the byte array a with the data of the AudioInputStream
			ais.read(a);

			data = new int[a.length / 2];

			// convert byte array, a, into int array, data
			for (int i = 0; i < data.length; ++i)
			{
				/* First byte is HB (most significant digits) - coerce to 32-bit int */
				// HB =def sign_extend(a[2*i]) from 8 bit byte to 32 bit int
				int HB = (int) a[2 * i];
				/* Second byte is LB (least significant digits) - coerce to 32-bit int */
				// LB =def sign_extend(a[2*i+1]) from 8 bit byte to 32 bit int
				int LB = (int) a[2 * i + 1];
				// note that data[i] =def sign_extend(HB.LB)
				// | : Bool^32 x Bool^32 -----> Bool^32 where Bool = {0, 1}
				data[i] = HB << 8 | (LB & 0xff);
			}

			// create new int arrays for input and output channels of the right size
			ich0 = new int[data.length / numChannels];
			ich1 = new int[data.length / numChannels];

			och0 = new int[ich0.length * 2];
			och1 = new int[och0.length];

			// fill up ich0 and ich1 by splitting data
			for (int i = 0; i < data.length / 2; i++)
			{
				ich0[i] = data[i * 2];
				ich1[i] = data[i * 2 + 1];
			}

			//
			// ----------------------------------------------------------------------------
			// compute the output channels from the input channels - this is the hard part:

			int N = frameInterval * frameSize / 2;
			// ints per segment Ai (or Bi)

			int L = ich0.length;
			// length of (either) input array
			int outL = L * 2;
			// length of (either) output array

			// index i marks out start positions of double segments Ai O, (or Bi O) each of
			// length 2*N
			// index j counts individual bytes in segments Ai, each of length N, going from
			// 0 to N-1
			// index k counts individual bytes in the final segment E (or F), of length R =
			// outL % N, going from 0 to R-1
			int R = (outL % (2 * N)) / 2;
			//System.out.println("outL = " + outL + ", N = " + N + ", R (outL % N) = " + R);

			// MAIN CODE HERE MAKING USE OF i, j, k, N, R ?? .....
			for (int i = 0; i < (double) outL / (double) (2 * N) - 1; i++)
			{
				//System.out.println("new j loop, start: " + (i * 2 * N) + ", end: " + ((i * 2 * N) + N + N - 1));
				for (int j = 0; j < N; j++)
				{
					// First half (A0 in ch0, O in ch1)
					och0[(i * 2 * N) + j] = ich0[(i * N) + j];
					och1[(i * 2 * N) + j] = 0x00;

					// Second half (B0 in ch1, O in ch0)
					och0[(i * 2 * N) + j + N] = 0x00;
					och1[(i * 2 * N) + j + N] = ich1[(i * N) + j];
				}
			}

			int lastSeg = (int) Math.floor((double) outL / (double) (2 * N));

			//System.out.println("last segment, start: " + (lastSeg * 2 * N) + ", end: " + ((lastSeg * 2 * N) + R + R - 1));
			for (int k = 0; k < R; k++)
			{
				och0[(lastSeg * 2 * N) + k] = ich0[(lastSeg * N) + k];
				och1[(lastSeg * 2 * N) + k] = 0x00;

				och0[(lastSeg * 2 * N) + k + R] = 0x00;
				och1[(lastSeg * 2 * N) + k + R] = ich1[(lastSeg * N) + k];
			}

			// END OF compute the output channels from the input channels //
			// ----------------------------------------------------------------------------

			// finally ... join och0 and och1 into b for (int i=0; i < b.length; i += 4) {
			// b[i] = och0[i/2];
			// etc etc }

			// combine output channels into int array, data
			data = new int[och0.length * 2];
			for (int i = 0; i < data.length; i += 2)
			{
				data[i] = och0[i / 2];
				data[i + 1] = och1[i / 2];
			}

			// get the maximum amplitute
			int max = 0;
			for (int i = 0; i < data.length; ++i)
			{
				max = Math.max(max, Math.abs(data[i]));
			}

			// 16 digit 2s-complement range from -2^15 to +2^15-1 = 256*128-1
			// therefore we linearly scale data[i] values to lie within this range ..
			// .. so that each data[i] has a 16 digit "HB.LB binary representation"
			if (max > 256 * 128 - 1)
			{
				System.out.println("Sound values are linearly scaled by " + (256.0 * 128.0 - 1) / max
						+ " because maximum amplitude is larger than upper boundary of allowed value range.");
				for (int i = 0; i < data.length; ++i)
				{
					data[i] = (int) (data[i] * (256.0 * 128.0 - 1) / max);
				}
			}

			// convert the integer array to a byte array
			for (int i = 0; i < data.length; ++i)
			{
				b[2 * i] = (byte) ((data[i] >> 8) & 0xff);
				b[2 * i + 1] = (byte) (data[i] & 0xff);
			}

			/*
			 * ----- template code commented out END
			 */

			// fill up b using och0 and och1
			// for (int i=0; i < b.length; i += 4) {
			// b[i] = och0[i/2];
			// ??
			// }

		} catch (Exception e)
		{
			System.out.println("Exception occured in altChannels()");
			e.printStackTrace();
		}

		// return b
		return new AudioInputStream(new ByteArrayInputStream(b), ais.getFormat(),
				b.length / ais.getFormat().getFrameSize());

	} // end altChannels

} // AudioManipulation
