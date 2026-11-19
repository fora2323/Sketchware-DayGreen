package <?package_name?>;

import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import rikka.shizuku.Shizuku;

public final class IDShizuku {

	private IDShizuku() {
	}

	public static boolean pingBinder() {
		return Shizuku.pingBinder();
	}

	public static boolean checkSelfPermission() {
		return pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
	}

	public static void requestPermission(int requestCode) {
		if (pingBinder()) {
			Shizuku.requestPermission(requestCode);
		}
	}

	public static void addRequestPermissionResultListener(@NonNull Shizuku.OnRequestPermissionResultListener listener) {
		Shizuku.addRequestPermissionResultListener(listener);
	}

	public static void removeRequestPermissionResultListener(@NonNull Shizuku.OnRequestPermissionResultListener listener) {
		Shizuku.removeRequestPermissionResultListener(listener);
	}
}