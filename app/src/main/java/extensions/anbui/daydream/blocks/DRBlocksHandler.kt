package extensions.anbui.daydream.blocks

object DRBlockHandler {
    @JvmStatic
    fun addBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
        addViewBlocks(arrayList)
        addStringBlocks(arrayList)
        addStringOperatorBlocks(arrayList)
        addSharedPreferencesBlocks(arrayList)
        addBasicComponentBlocks(arrayList)
        addIntentPutExtraBlocks(arrayList)
    }

    @JvmStatic
    fun addViewBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "setBackgroundResource"
        hashMap["type"] = " "
        hashMap["code"] = "%s.setBackgroundResource(R.drawable.%s);"
        hashMap["color"] = "#4A6CD4"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.view setBackgroundResource %m.drawable"
        arrayList.add(hashMap)
    }

    @JvmStatic
    fun addStringBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
        var hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "concatenateVarString"
        hashMap["type"] = " "
        hashMap["code"] = "%s += %s;"
        hashMap["color"] = "#EE7D16"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.varStr append %s"
        arrayList.add(hashMap)

//        hashMap = HashMap()
//        hashMap["name"] = "setVarStringWithCondition"
//        hashMap["type"] = "s"
//        hashMap["code"] = "(%s) ? %s : %s"
//        hashMap["color"] = "#EE7D16"
//        hashMap["palette"] = "-1"
//        hashMap["spec"] = "if %b then %s else %s"
//        arrayList.add(hashMap)
//
//        hashMap = HashMap()
//        hashMap["name"] = "varStringIsEmpty"
//        hashMap["type"] = "b"
//        hashMap["code"] = "%s.isEmpty()"
//        hashMap["color"] = "#EE7D16"
//        hashMap["palette"] = "-1"
//        hashMap["spec"] = "%m.varStr isEmpty"
//        arrayList.add(hashMap)
    }

    @JvmStatic
    fun addStringOperatorBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
//        val hashMap: HashMap<String, Any> = HashMap()
//        hashMap["name"] = "stringIsEmpty"
//        hashMap["type"] = "b"
//        hashMap["code"] = "%s.isEmpty()"
//        hashMap["color"] = "#5CB722"
//        hashMap["palette"] = "-1"
//        hashMap["spec"] = "%s isEmpty"
//        arrayList.add(hashMap)
    }

    @JvmStatic
    fun addSharedPreferencesBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
        var hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "getStringSharedPreferences"
        hashMap["type"] = "s"
        hashMap["code"] = "%s.getString(%s, %s)"
        hashMap["color"] = "#2CA5E2"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.file getString key %s defaultValue %s"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "putStringSharedPreferences"
        hashMap["type"] = " "
        hashMap["code"] = "%s.edit().putString(%s, %s).apply();"
        hashMap["color"] = "#2CA5E2"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.file putString key %s value %s"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "getBooleanSharedPreferences"
        hashMap["type"] = "b"
        hashMap["code"] = "%s.getBoolean(%s, %s)"
        hashMap["color"] = "#2CA5E2"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.file getBoolean key %s defaultValue %b"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "putBooleanSharedPreferences"
        hashMap["type"] = " "
        hashMap["code"] = "%s.edit().putBoolean(%s, %s).apply();"
        hashMap["color"] = "#2CA5E2"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.file putBoolean key %s value %b"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "getIntSharedPreferences"
        hashMap["type"] = "d"
        hashMap["code"] = "%s.getInt(%s, %s)"
        hashMap["color"] = "#2CA5E2"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.file getInt key %s defaultValue %d"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "putIntSharedPreferences"
        hashMap["type"] = " "
        hashMap["code"] = "%s.edit().putInt(%s, (int) %s).apply();"
        hashMap["color"] = "#2CA5E2"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.file putInt key %s value %d"
        arrayList.add(hashMap)
    }

    @JvmStatic
    fun addBasicComponentBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
        var hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "intentGetBoolean"
        hashMap["type"] = "b"
        hashMap["code"] = "getIntent().getBooleanExtra(%s, false)"
        hashMap["color"] = "#2CA5E2"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "Activity getExtra key %s"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "intentGetDouble"
        hashMap["type"] = "d"
        hashMap["code"] = "getIntent().getDoubleExtra(%s, 0.0)"
        hashMap["color"] = "#2CA5E2"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "Activity getExtra key %s"
        arrayList.add(hashMap)
    }

    @JvmStatic
    fun addIntentPutExtraBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
        var hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "intentPutExtraBoolean"
        hashMap["type"] = " "
        hashMap["code"] = "%s.putExtra(%s, %s);"
        hashMap["color"] = "#2CA5E2"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.intent putExtra key %s value %b"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "intentPutExtraDouble"
        hashMap["type"] = " "
        hashMap["code"] = "%s.putExtra(%s, (double) %s);"
        hashMap["color"] = "#2CA5E2"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.intent putExtra key %s value %d"
        arrayList.add(hashMap)
    }
}