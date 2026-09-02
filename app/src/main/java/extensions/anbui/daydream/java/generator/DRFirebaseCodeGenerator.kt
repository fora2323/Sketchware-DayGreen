package extensions.anbui.daydream.java.generator

import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.java.generator.DRJavaCodeGenerator.isUseLambda
import extensions.anbui.daydream.java.generator.DRJavaCodeGenerator.processNewListenerEventLogicCodeWithLambda

object DRFirebaseCodeGenerator {
    /**
     * Auth
     */
    @JvmStatic
    fun authUpdateEmailComplete(targetId: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            createWithLambda("${targetId}_updateEmailListener", logic)
        } else {
            "${targetId}_updateEmailListener = new OnCompleteListener<Void>() {\r\n" +
                    logic + "\r\n" +
                    "};"
        }
    }

    @JvmStatic
    fun authUpdatePasswordComplete(targetId: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            createWithLambda("${targetId}_updatePasswordListener", logic)
        } else {
            "${targetId}_updatePasswordListener = new OnCompleteListener<Void>() {\r\n" +
                    logic + "\r\n" +
                    "};"
        }
    }

    @JvmStatic
    fun authEmailVerificationSent(targetId: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            createWithLambda("${targetId}_emailVerificationSentListener", logic)
        } else {
            "${targetId}_emailVerificationSentListener = new OnCompleteListener<Void>() {\r\n" +
                    logic + "\r\n" +
                    "};"
        }
    }

    @JvmStatic
    fun authDeleteUserComplete(targetId: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            createWithLambda("${targetId}_deleteUserListener", logic)
        } else {
            "${targetId}_deleteUserListener = new OnCompleteListener<Void>() {\r\n" +
                    logic + "\r\n" +
                    "};"
        }
    }

    @JvmStatic
    fun authsignInWithPhoneAuth(targetId: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            createWithLambda("${targetId}_phoneAuthListener", logic)
        } else {
            "${targetId}_phoneAuthListener = new OnCompleteListener<AuthResult>() {\r\n" +
                    logic + "\r\n" +
                    "};"
        }
    }

    @JvmStatic
    fun authUpdateProfileComplete(targetId: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            createWithLambda("${targetId}_updateProfileListener", logic)
        } else {
            "${targetId}_updateProfileListener = new OnCompleteListener<Void>() {\r\n" +
                    logic + "\r\n" +
                    "};"
        }
    }

    @JvmStatic
    fun googleSignInListener(targetId: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            createWithLambda("${targetId}_googleSignInListener", logic)
        } else {
            "${targetId}_googleSignInListener = new OnCompleteListener<AuthResult>() {\r\n" +
                    logic + "\r\n" +
                    "};"
        }
    }

    @JvmStatic
    fun authCreateUserComplete(targetId: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            createWithLambda("_${targetId}_create_user_listener", logic)
        } else {
            "_${targetId}_create_user_listener = new OnCompleteListener<AuthResult>() {\r\n" +
                    logic + "\r\n" +
                    "};"
        }
    }

    @JvmStatic
    fun authSignInUserComplete(targetId: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            createWithLambda("_${targetId}_sign_in_listener", logic)
        } else {
            "_${targetId}_sign_in_listener = new OnCompleteListener<AuthResult>() {\r\n" +
                    logic + "\r\n" +
                    "};"
        }
    }

    @JvmStatic
    fun authResetEmailSent(targetId: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            createWithLambda("_${targetId}_reset_password_listener", logic)
        } else {
            "_${targetId}_reset_password_listener = new OnCompleteListener<Void>() {\r\n" +
                    logic + "\r\n" +
                    "};"
        }
    }


    /**
     * Core
     */
    @JvmStatic
    fun createWithLambda(componentName: String, logic: String): String {
        return processNewListenerEventLogicCodeWithLambda(
            componentName,
            if (logic.contains("task.isSuccessful()")) "task" else "_param1", logic,
            "public void onComplete"
        )
    }
}