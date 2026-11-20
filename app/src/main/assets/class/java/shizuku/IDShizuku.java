package <?package_name?>;

import android.content.Context;
import android.content.pm.PackageManager;
import java.io.InputStream;
import java.io.OutputStream;
import androidx.annotation.NonNull;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import rikka.shizuku.Shizuku;

public final class IDShizuku {

	private final Context context;

	public IDShizuku(Context context) {
		this.context = context;
	}

	public boolean pingBinder() {
		return Shizuku.pingBinder();
	}

	public boolean checkSelfPermission() {
		return pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
	}

	public void requestPermission(int requestCode) {
		if (pingBinder()) {
			Shizuku.requestPermission(requestCode);
		}
	}

	public int getVersion() {
		return Shizuku.getVersion();
	}

	public int getUid() {
		return Shizuku.getUid();
	}    

	public void setCopyFromAsset(String assetName, String destinationFileName, String destinationPath) {
		try {
			InputStream inputStream = context.getAssets().open(assetName);
			java.lang.Process process = Shizuku.newProcess(new String[]{"sh", "-c", "cat > \"" + destinationPath + "/" + destinationFileName + "\""}, null, null);
			OutputStream outputStream = process.getOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, length);
			}
			outputStream.close();
			inputStream.close();
			process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setDeletePath(final String delete) {
		try {
			java.lang.Process process = Shizuku.newProcess(new String[]{"sh", "-c", "rm -rf " + delete}, null, null);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			StringBuilder output = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addRequestPermissionResultListener(@NonNull Shizuku.OnRequestPermissionResultListener listener) {
		Shizuku.addRequestPermissionResultListener(listener);
	}
}