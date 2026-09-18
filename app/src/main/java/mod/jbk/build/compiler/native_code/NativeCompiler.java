package mod.jbk.build.compiler.native_code;

import a.a.a.ProjectBuilder;
import mod.jbk.build.BuildProgressReceiver;

public class NativeCompiler {
    private final ProjectBuilder projectBuilder;
    private final BuildProgressReceiver progressReceiver;

    public NativeCompiler(ProjectBuilder projectBuilder, BuildProgressReceiver progressReceiver) {
        this.projectBuilder = projectBuilder;
        this.progressReceiver = progressReceiver;
    }

    public void compile() throws Exception {
        // Experimental native C/C++ compilation
    }
}
