package extensions.anbui.daydream.java.generator

import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.project.ProjectBuildConfigs
import extensions.anbui.daydream.utils.TextUtils

object DRJavaCodeGenerator {
    /**
     * If else
     */
    @JvmStatic
    fun ifLogic(condition: String, logic: String): String {
        if (logic.isEmpty() || condition == "false") return ""

        if (condition == "true") {
            return logic
        }

        return if (isUseSingleLineLambda(logic)) {
            "if ($condition) $logic"
        } else {
            "if ($condition) {\r\n$logic\r\n}"
        }
    }

    @JvmStatic
    fun ifElseLogic(condition: String, logicIf: String, logicElse: String): String {
        if (logicIf.isEmpty() && logicElse.isEmpty()) {
            return ""
        } else if (!logicIf.isEmpty() && logicElse.isEmpty()) {
            return ifLogic(condition, logicIf)
        } else if (logicIf.isEmpty() && !logicElse.isEmpty()) {
            return ifLogic("!($condition)", logicElse)
        }

        if (condition == "true") {
            return logicIf
        } else if (condition == "false") {
            return logicElse
        }

        val isSingleLine = isUseSingleLineLambda(logicIf) && isUseSingleLineLambda(logicElse)

        return if (logicElse.isEmpty()) {
            ifLogic(condition, logicIf)
        } else if (logicIf.isEmpty()) {
            ifLogic("!($condition)", logicElse)
        } else {
            if (isSingleLine) {
                "if ($condition) $logicIf else $logicElse"
            } else {
                "if ($condition) {\r\n$logicIf\r\n} else {\r\n$logicElse\r\n}"
            }
        }
    }


    /**
     * Timer
     */
    @JvmStatic
    fun timerDelay(isActivity: Boolean, componentName: String, logic: String, delay: String): String {
        if (logic.isEmpty()) {
            return ""
        }

        return if (TextUtils.isNumberOnly(delay) && Integer.parseInt(delay) == 0) {
            logic
        } else {
            "$componentName = new TimerTask() {\r\n@Override\r\npublic void run() {\r\n${
                runOnUiThread(
                    isActivity,
                    logic
                )
            }\r\n}\r\n};\r\n_timer.schedule($componentName, " +
                    (if (TextUtils.isValidInteger(delay))
                        delay
                    else
                        "(int) $delay") +
                    ");"
        }

    }

    @JvmStatic
    fun timerRepeatEvery(
        isActivity: Boolean,
        componentName: String,
        logic: String,
        delay: String,
        every: String
    ): String {
        if (logic.isEmpty()) {
            return ""
        }

        return "$componentName = new TimerTask() {\r\n@Override\r\npublic void run() {\r\n${
            runOnUiThread(
                isActivity,
                logic
            )
        }\r\n}\r\n};\r\n_timer.scheduleAtFixedRate($componentName, " +
                (if (TextUtils.isValidInteger(delay))
                    delay
                else
                    "(int) $delay") +
                ", " +
                (if (TextUtils.isValidInteger(every))
                    every
                else
                    "(int) $every") +
                ");"
    }

    @JvmStatic
    fun timerCancel(componentName: String): String {
        return "if ($componentName != null) {\r\ntry { $componentName.cancel(); } catch (Exception ignored) {}\r\n}"
    }


    /**
     * Click
     */
    @JvmStatic
    fun setOnClickListenerEvent(componentName: String, logic: String): String {
        val processedLogic = logic.substringAfter("{").substringBeforeLast("}").trim()

        return if (processedLogic.isEmpty()) {
            ""
        } else if (isUseLambda(Configs.currentProjectID)) {
            processEventLogicCodeWithLambda(
                componentName,
                "setOnClickListener(_v",
                logic,
                "public void onClick"
            )
        } else {
            componentName + ".setOnClickListener(new View.OnClickListener() {\r\n" +
                    logic +
                    "\r\n});"
        }
    }

    @JvmStatic
    fun setOnLongClickListenerEvent(componentName: String, logic: String): String {
        val processedLogic = logic.substringAfter("{").substringBeforeLast("}").trim()

        return if (processedLogic.isEmpty()) {
            ""
        } else if (isUseLambda(Configs.currentProjectID)) {
            processEventLogicCodeWithLambda(
                componentName,
                "setOnLongClickListener(_v",
                logic,
                "public boolean onLongClick"
            )
        } else {
            componentName + ".setOnLongClickListener(new View.OnLongClickListener() {\r\n" +
                    logic +
                    "\r\n});"
        }
    }


    /**
     * Switch and checkbox
     */
    @JvmStatic
    fun setOnCheckedChangedListenerEvent(componentName: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            processEventLogicCodeWithLambda(
                componentName,
                "setOnCheckedChangeListener((_buttonView, _isChecked)",
                logic.replace("final boolean _isChecked = _param2;\r\n", ""),
                "public void onCheckedChanged"
            )
        } else {
            componentName + ".setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {\r\n" +
                    logic +
                    "\r\n});"
        }
    }


    /**
     * Admob
     */
    @JvmStatic
    fun setOnUserEarnedRewardListenerEvent(rewardedAdID: String, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            processNewListenerEventLogicCodeWithLambda(
                "_" + rewardedAdID + "_on_user_earned_reward_listener",
                "_param1",
                "int _rewardAmount = _param1.getAmount();\r\n" +
                        "String _rewardType = _param1.getType();\r\n" +
                        logic,
                "public void onUserEarnedReward"
            )
        } else {
            "_" + rewardedAdID + "_on_user_earned_reward_listener = new OnUserEarnedRewardListener() {\r\n" +
                    "@Override\r\npublic void onUserEarnedReward(RewardItem _param1) {\r\n" +
                    "int _rewardAmount = _param1.getAmount();\r\n" +
                    "String _rewardType = _param1.getType();\r\n" +
                    logic + "\r\n" +
                    "}\r\n};"
        }
    }

    /**
     * Core
     */
    @JvmStatic
    fun processEventLogicCodeWithLambda(
        componentName: String,
        eventListener: String,
        logic: String,
        eventInside: String
    ): String {
        var processedLogic = logic

        if (!logic.isEmpty()) {
            processedLogic = processedLogic.replaceFirst(
                Regex("^\\s*@Override\\s*\\r?\\n", RegexOption.MULTILINE),
                ""
            )

            processedLogic = processedLogic.replaceFirst(
                Regex("^\\s*$eventInside\\([^)]*\\)\\s*\\{\\s*\\r?\\n", RegexOption.MULTILINE),
                ""
            )

            processedLogic = processedLogic.substringBeforeLast("\r\n}")

            if (processedLogic.trim() == "}") {
                processedLogic = ""
            }
        }

        val isSingleLine = isUseSingleLineLambda(processedLogic)

        return "$componentName.$eventListener -> " +
                (if (isSingleLine) "" else "{\r\n") +
                (if (isSingleLine) processedLogic.substringBeforeLast(";") else processedLogic) +
                (if (isSingleLine) "" else "\r\n}") +
                ");"
    }

    @JvmStatic
    fun processNewListenerEventLogicCodeWithLambda(
        componentName: String,
        newVaribles: String,
        logic: String,
        eventInside: String
    ): String {
        var processedLogic = logic

        if (!logic.isEmpty()) {
            processedLogic = processedLogic.replaceFirst(
                Regex("^\\s*@Override\\s*\\r?\\n", RegexOption.MULTILINE),
                ""
            )

            processedLogic = processedLogic.replaceFirst(
                Regex("^\\s*$eventInside\\([^)]*\\)\\s*\\{\\s*\\r?\\n", RegexOption.MULTILINE),
                ""
            )

            processedLogic = processedLogic.substringBeforeLast("\r\n}")
        }

        val isSingleLine = isUseSingleLineLambda(processedLogic)

        return "$componentName = $newVaribles -> " +
                (if (isSingleLine) "" else "{\r\n") +
                processedLogic +
                (if (isSingleLine) "" else "\r\n};")
    }

    @JvmStatic
    fun isUseSingleLineLambda(logic: String): Boolean {
        val lines = logic.lines().filter { it.isNotBlank() }

        if (lines.size == 1 && !logic.isEmpty()) {
            val numberOfSemicolonsOutsideQuotationMarks =
                TextUtils.countACharacterOutsideCharacterPairs(logic.replace("\\\"", ""), ';', "\"")

            return numberOfSemicolonsOutsideQuotationMarks <= 1 &&
                    !logic.startsWith("//") &&
                    !logic.startsWith("*/") &&
                    !logic.startsWith("if (") &&
                    !logic.startsWith("for (") &&
                    !logic.startsWith("while (") &&
                    !logic.startsWith("do ") &&
                    !logic.startsWith("try ") &&
                    !logic.startsWith("final ")
        }

        return false
    }

    @JvmStatic
    fun isUseLambda(projectID: String): Boolean {
        return !ProjectBuildConfigs.isUseJava7(projectID)
    }


    /**
     * Number
     */
    @JvmStatic
    fun compareEqualNumbers(number1: String, number2: String): String {
        var processedLogic = "$number1 == $number2"

        if (processedLogic.contains(".length()") &&
            ((TextUtils.isValidInteger(number1) && Integer.parseInt(number1) == 0) || (TextUtils.isValidInteger(
                number2
            ) && Integer.parseInt(number2) == 0))
        ) {
            processedLogic = processedLogic.replace(".length()", ".isEmpty()")
            processedLogic = processedLogic.replace(" == 0", "")
            processedLogic = processedLogic.replace("0 == ", "")
        }

        return processedLogic
    }

    @JvmStatic
    fun compareSmallerNumbers(number1: String, number2: String): String {
        var processedLogic = "$number1 < $number2"

        if (processedLogic.contains(".length()") && (TextUtils.isValidInteger(number2) && Integer.parseInt(
                number2
            ) == 1)
        ) {
            processedLogic = processedLogic.replace(".length()", ".isEmpty()")
            processedLogic = processedLogic.replace(" < 1", "")
        }

        return processedLogic
    }

    @JvmStatic
    fun compareLargerNumbers(number1: String, number2: String): String {
        var processedLogic = "$number1 > $number2"

        if (processedLogic.contains(".length()") && (TextUtils.isValidInteger(number2) && Integer.parseInt(
                number2
            ) == 0)
        ) {
            processedLogic = "!$processedLogic"
            processedLogic = processedLogic.replace(".length()", ".isEmpty()")
            processedLogic = processedLogic.replace(" > 0", "")
        }

        return processedLogic
    }

    @JvmStatic
    fun stringSub(componentName: String, from: String, to: String): String {
        return "$componentName.substring(${castToIntIfNeeded(from)}, ${castToIntIfNeeded(to)})"
    }


    /**
     * runOnUiThread
     */
    @JvmStatic
    fun runOnUiThread(isActivity: Boolean, logic: String): String {
        return if (isUseLambda(Configs.currentProjectID)) {
            if (isUseSingleLineLambda(logic)) {
                (if (isActivity) "" else "if (!isAdded()) return;\r\nrequireActivity().") + "runOnUiThread(() -> ${logic.dropLast(1)});"
            } else {
                (if (isActivity) "" else "if (!isAdded()) return;\r\nrequireActivity().") + "runOnUiThread(() -> {\r\n$logic\r\n});"
            }
        } else {
            (if (isActivity) "" else "if (!isAdded()) return;\r\nrequireActivity().") + "runOnUiThread(new Runnable() {\r\n@Override\r\npublic void run() {\r\n$logic\r\n}\r\n});"
        }
    }

    @JvmStatic
    fun castToIntIfNeeded(value: String): String {
        return (if (TextUtils.isValidInteger(value)) "" else "(int) ") + value
    }

    @JvmStatic
    fun castToFloatIfNeeded(value: String): String {
        var validValue = value.lowercase()
        if (validValue.endsWith("d")) {
            validValue = validValue.removeSuffix("d") + "f"
        } else if (!validValue.endsWith("f")) {
            validValue += "f"
        }
        return if (TextUtils.isValidFloat(validValue)) {
            validValue
        } else {
            "(float) ($value)"
        }
    }
}