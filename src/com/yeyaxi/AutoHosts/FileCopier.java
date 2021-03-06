package com.yeyaxi.AutoHosts;

import android.os.AsyncTask;
import android.util.Log;
import com.yeyaxi.AutoHosts.R;

import java.io.*;

public class FileCopier extends AsyncTask<Object, Void, Boolean>
{
	private AutoHostsActivity callback;
	private int completionMessage;
	private boolean append;

	public FileCopier (AutoHostsActivity callback, int completionMessage, boolean append)
	{
		this.completionMessage = completionMessage;
		this.append = append;
		this.callback = callback;
	}

	@Override
	protected Boolean doInBackground (Object... inputs)
	{

		BufferedReader bufferedReader = null;
		Process process = null;
		try
		{
			String[] mountLocation = SystemMount.getMountLocation();

			final Runtime runtime = Runtime.getRuntime();
			process = runtime.exec("su");

			DataOutputStream os = new DataOutputStream(process.getOutputStream());
			os.writeBytes("mount -o rw,remount -t " + mountLocation[1] + " " + mountLocation[0] + " /system\n");

			bufferedReader = new BufferedReader(getReader(inputs[0]));

			os.writeBytes("echo '' > " + inputs[1] + "\n");
			String line = null;
			while((line = bufferedReader.readLine()) != null)
				os.writeBytes("echo '" + line + "' >> " + inputs[1] + "\n");


			os.writeBytes("mount -o ro,remount -t " + mountLocation[1] + " " + mountLocation[0] + " /system\n");
			os.writeBytes("exit\n");
			os.flush();
			os.close();

			process.waitFor();

			int exitValue = process.exitValue();
			Log.d(Constants.LOG_NAME, "Exit Value For File Copier: " + exitValue);
			if (exitValue != 255 && exitValue != 126)
				return Boolean.TRUE;

		} catch (InterruptedException ex)
		{
			Log.e(Constants.LOG_NAME, ex.getMessage(), ex);
		} catch (IOException ex)
		{
			Log.e(Constants.LOG_NAME, ex.getMessage(), ex);
		} catch (UnableToMountSystemException ex)
		{
			Log.e(Constants.LOG_NAME, ex.getMessage(), ex);
		} finally
		{
			closeStream(bufferedReader);
			if(process != null)
				process.destroy();
		}

		return Boolean.FALSE;
	}

	private Reader getReader (Object input) throws FileNotFoundException
	{
		if(input instanceof  InputStream)
			return new InputStreamReader((InputStream) input);
		if(input instanceof  String)
			return new FileReader((String)input);
		else throw new FileNotFoundException("Unknown file type. " + input);
	}

	private void closeStream (Closeable closeable)
	{
		try
		{
			if (closeable != null)
				closeable.close();
		} catch (IOException ex)
		{
			Log.e(Constants.LOG_NAME, "Error closing stream. ", ex);
		}
	}

	@Override
	protected void onPostExecute (Boolean success)
	{
		if (success)
		{
			callback.displayCalbackMessage(completionMessage, R.string.append_success);
			callback.doNextTask();
		} else
			callback.displayCalbackErrorMessage(completionMessage);
	}

}
