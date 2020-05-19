import android.content.pm.IPackageInstallObserver;
import android.net.Uri;

interface IPackageManager {
    /**
     * Install a package.
     *
     * @param packageURI The location of the package file to install.
     * @param observer a callback to use to notify when the package installation in finished.
     * @param flags - possible values: {@link #FORWARD_LOCK_PACKAGE},
     * {@link #REPLACE_EXISITING_PACKAGE}
     * @param installerPackageName Optional package name of the application that is performing the
     * installation. This identifies which market the package came from.
     */
    void installPackage(in Uri packageURI, IPackageInstallObserver observer, int flags,
            in String installerPackageName);
}
