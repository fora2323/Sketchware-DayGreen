package <?package_name?>;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

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

	public void addRequestPermissionResultListener(@NonNull Shizuku.OnRequestPermissionResultListener listener) {
		Shizuku.addRequestPermissionResultListener(listener);
	}
}