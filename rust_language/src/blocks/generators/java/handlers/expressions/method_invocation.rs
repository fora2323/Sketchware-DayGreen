use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use crate::blocks::generators::java::handlers::fallback_stmt::FallbackStatement;
use tree_sitter::Node;

pub struct MethodInvocationHandler;

impl StatementHandler for MethodInvocationHandler {
    fn get_block(kind: &str) -> BlockBean {
        match kind {
            "doToast" => BlockBean::new()
                .set_op_code("doToast")
                .set_type(" ")
                .set_spec("Toast %s")
                .set_code("SketchwareUtil.showMessage(getApplicationContext(), %s);")
                .set_color(Some(0xFF2CA5E2)),
            "finishActivity" => BlockBean::new()
                .set_op_code("finishActivity")
                .set_type(" ")
                .set_spec("finish activity")
                .set_code("finish();")
                .set_color(Some(0xFF2CA5E2)),
            "setText" => BlockBean::new()
                .set_op_code("setText")
                .set_type(" ")
                .set_spec("%m.textview setText %s")
                .set_code("%s.setText(%s);")
                .set_color(Some(0xFF4A6CD4)),
            "getText" => BlockBean::new()
                .set_op_code("getText")
                .set_type("s")
                .set_spec("%m.textview getText")
                .set_code("%s.getText().toString()")
                .set_color(Some(0xFF4A6CD4)),
            "setVisible" => BlockBean::new()
                .set_op_code("setVisible")
                .set_type(" ")
                .set_spec("%m.view setVisible %m.visible")
                .set_code("%s.setVisibility(%s);")
                .set_color(Some(0xFF4A6CD4)),
            "setEnable" => BlockBean::new()
                .set_op_code("setEnable")
                .set_type(" ")
                .set_spec("%m.view setEnabled %b")
                .set_code("%s.setEnabled(%s);")
                .set_color(Some(0xFF4A6CD4)),
            "getEnable" => BlockBean::new()
                .set_op_code("getEnable")
                .set_type("b")
                .set_spec("%m.view isEnabled")
                .set_code("%s.isEnabled()")
                .set_color(Some(0xFF4A6CD4)),
            "setChecked" => BlockBean::new()
                .set_op_code("setChecked")
                .set_type(" ")
                .set_spec("%m.compoundButton setChecked %b")
                .set_code("%s.setChecked(%s);")
                .set_color(Some(0xFF4A6CD4)),
            "getChecked" => BlockBean::new()
                .set_op_code("getChecked")
                .set_type("b")
                .set_spec("%m.compoundButton isChecked")
                .set_code("%s.isChecked()")
                .set_color(Some(0xFF4A6CD4)),
            "setRotate" => BlockBean::new()
                .set_op_code("setRotate")
                .set_type(" ")
                .set_spec("%m.view setRotation %d")
                .set_code("%s.setRotation((float)(%s));")
                .set_color(Some(0xFF4A6CD4)),
            "setAlpha" => BlockBean::new()
                .set_op_code("setAlpha")
                .set_type(" ")
                .set_spec("%m.view setAlpha %d")
                .set_code("%s.setAlpha((float)(%s));")
                .set_color(Some(0xFF4A6CD4)),
            "setTextColor" => BlockBean::new()
                .set_op_code("setTextColor")
                .set_type(" ")
                .set_spec("%m.textview setTextColor %d")
                .set_code("%s.setTextColor(%s);")
                .set_color(Some(0xFF4A6CD4)),
            "setBgColor" => BlockBean::new()
                .set_op_code("setBgColor")
                .set_type(" ")
                .set_spec("%m.view setBackgroundColor %d")
                .set_code("%s.setBackgroundColor(%s);")
                .set_color(Some(0xFF4A6CD4)),
            "setImage" => BlockBean::new()
                .set_op_code("setImage")
                .set_type(" ")
                .set_spec("%m.imageview setImageResource %m.resource")
                .set_code("%s.setImageResource(R.drawable.%s);")
                .set_color(Some(0xFF4A6CD4)),
            "intentSetScreen" => BlockBean::new()
                .set_op_code("intentSetScreen")
                .set_type(" ")
                .set_spec("%m.intent setScreen %m.activity")
                .set_code("%s.setClass(getApplicationContext(), %s.class);")
                .set_color(Some(0xFF2CA5E2)),
            "startActivity" => BlockBean::new()
                .set_op_code("startActivity")
                .set_type(" ")
                .set_spec("startActivity %m.intent")
                .set_code("startActivity(%s);")
                .set_color(Some(0xFF2CA5E2)),
            "intentPutExtra" => BlockBean::new()
                .set_op_code("intentPutExtra")
                .set_type(" ")
                .set_spec("%m.intent putExtra key %s value %s")
                .set_code("%s.putExtra(%s, %s);")
                .set_color(Some(0xFF2CA5E2)),
            "intentSetAction" => BlockBean::new()
                .set_op_code("intentSetAction")
                .set_type(" ")
                .set_spec("%m.intent setAction %s")
                .set_code("%s.setAction(%s);")
                .set_color(Some(0xFF2CA5E2)),
            "stringEquals" => BlockBean::new()
                .set_op_code("stringEquals")
                .set_type("b")
                .set_spec("%s equals %s")
                .set_code("%s.equals(%s)")
                .set_color(Some(0xFF5CB722)),
            "stringContains" => BlockBean::new()
                .set_op_code("stringContains")
                .set_type("b")
                .set_spec("%s contains %s")
                .set_code("%s.contains(%s)")
                .set_color(Some(0xFF5CB722)),
            "stringJoin" => BlockBean::new()
                .set_op_code("stringJoin")
                .set_type("s")
                .set_spec("%s join %s")
                .set_code("%s.concat(%s)")
                .set_color(Some(0xFF5CB722)),
            "stringLength" => BlockBean::new()
                .set_op_code("stringLength")
                .set_type("d")
                .set_spec("length of %s")
                .set_code("%s.length()")
                .set_color(Some(0xFF5CB722)),
            "stringIndex" => BlockBean::new()
                .set_op_code("stringIndex")
                .set_type("d")
                .set_spec("%s index of %s")
                .set_code("%s.indexOf(%s)")
                .set_color(Some(0xFF5CB722)),
            "stringLastIndex" => BlockBean::new()
                .set_op_code("stringLastIndex")
                .set_type("d")
                .set_spec("%s last index of %s")
                .set_code("%s.lastIndexOf(%s)")
                .set_color(Some(0xFF5CB722)),
            "stringSub" => BlockBean::new()
                .set_op_code("stringSub")
                .set_type("s")
                .set_spec("%s substring from %d to %d")
                .set_code("%s.substring((int)(%s), (int)(%s))")
                .set_color(Some(0xFF5CB722)),
            "trim" => BlockBean::new()
                .set_op_code("trim")
                .set_type("s")
                .set_spec("trim %s")
                .set_code("%s.trim()")
                .set_color(Some(0xFF5CB722)),
            "toUpperCase" => BlockBean::new()
                .set_op_code("toUpperCase")
                .set_type("s")
                .set_spec("%s to uppercase")
                .set_code("%s.toUpperCase()")
                .set_color(Some(0xFF5CB722)),
            "toLowerCase" => BlockBean::new()
                .set_op_code("toLowerCase")
                .set_type("s")
                .set_spec("%s to lowercase")
                .set_code("%s.toLowerCase()")
                .set_color(Some(0xFF5CB722)),
            "stringReplace" => BlockBean::new()
                .set_op_code("stringReplace")
                .set_type("s")
                .set_spec("%s replace %s with %s")
                .set_code("%s.replace(%s, %s)")
                .set_color(Some(0xFF5CB722)),
            "stringReplaceFirst" => BlockBean::new()
                .set_op_code("stringReplaceFirst")
                .set_type("s")
                .set_spec("%s replace first %s with %s")
                .set_code("%s.replaceFirst(%s, %s)")
                .set_color(Some(0xFF5CB722)),
            "stringReplaceAll" => BlockBean::new()
                .set_op_code("stringReplaceAll")
                .set_type("s")
                .set_spec("%s replace all %s with %s")
                .set_code("%s.replaceAll(%s, %s)")
                .set_color(Some(0xFF5CB722)),
            "mapPut" => BlockBean::new()
                .set_op_code("mapPut")
                .set_type(" ")
                .set_spec("%m.varMap put %s value %s")
                .set_code("%s.put(%s, %s);")
                .set_color(Some(0xFFEE7D16)),
            "mapGet" => BlockBean::new()
                .set_op_code("mapGet")
                .set_type("s")
                .set_spec("%m.varMap get %s")
                .set_code("%s.get(%s).toString()")
                .set_color(Some(0xFFEE7D16)),
            "mapContainKey" => BlockBean::new()
                .set_op_code("mapContainKey")
                .set_type("b")
                .set_spec("%m.varMap containsKey %s")
                .set_code("%s.containsKey(%s)")
                .set_color(Some(0xFFEE7D16)),
            "mapRemoveKey" => BlockBean::new()
                .set_op_code("mapRemoveKey")
                .set_type(" ")
                .set_spec("%m.varMap remove %s")
                .set_code("%s.remove(%s);")
                .set_color(Some(0xFFEE7D16)),
            "mapClear" => BlockBean::new()
                .set_op_code("mapClear")
                .set_type(" ")
                .set_spec("%m.varMap clear")
                .set_code("%s.clear();")
                .set_color(Some(0xFFEE7D16)),
            "mapIsEmpty" => BlockBean::new()
                .set_op_code("mapIsEmpty")
                .set_type("b")
                .set_spec("%m.varMap isEmpty")
                .set_code("%s.isEmpty()")
                .set_color(Some(0xFFEE7D16)),
            "mapSize" => BlockBean::new()
                .set_op_code("mapSize")
                .set_type("d")
                .set_spec("size of %m.varMap")
                .set_code("%s.size()")
                .set_color(Some(0xFFEE7D16)),
            "addListStr" => BlockBean::new()
                .set_op_code("addListStr")
                .set_type(" ")
                .set_spec("%m.listStr add %s")
                .set_code("%s.add(%s);")
                .set_color(Some(0xFFCC5B22)),
            "clearList" => BlockBean::new()
                .set_op_code("clearList")
                .set_type(" ")
                .set_spec("%m.list clear")
                .set_code("%s.clear();")
                .set_color(Some(0xFFCC5B22)),
            "lengthList" => BlockBean::new()
                .set_op_code("lengthList")
                .set_type("d")
                .set_spec("length of %m.list")
                .set_code("%s.size()")
                .set_color(Some(0xFFCC5B22)),
            "random" => BlockBean::new()
                .set_op_code("random")
                .set_type("d")
                .set_spec("pick random %d to %d")
                .set_code("SketchwareUtil.getRandom((int)(%s), (int)(%s))")
                .set_color(Some(0xFF5CB722)),
            "mathMin" => BlockBean::new()
                .set_op_code("mathMin")
                .set_type("d")
                .set_spec("min %d and %d")
                .set_code("Math.min(%s, %s)")
                .set_color(Some(0xFF23B9A9)),
            "mathMax" => BlockBean::new()
                .set_op_code("mathMax")
                .set_type("d")
                .set_spec("max %d and %d")
                .set_code("Math.max(%s, %s)")
                .set_color(Some(0xFF23B9A9)),
            "mathAbs" => BlockBean::new()
                .set_op_code("mathAbs")
                .set_type("d")
                .set_spec("abs %d")
                .set_code("Math.abs(%s)")
                .set_color(Some(0xFF23B9A9)),
            "mathSqrt" => BlockBean::new()
                .set_op_code("mathSqrt")
                .set_type("d")
                .set_spec("sqrt %d")
                .set_code("Math.sqrt(%s)")
                .set_color(Some(0xFF23B9A9)),
            "mathRound" => BlockBean::new()
                .set_op_code("mathRound")
                .set_type("d")
                .set_spec("round %d")
                .set_code("Math.round(%s)")
                .set_color(Some(0xFF23B9A9)),
            "mathCeil" => BlockBean::new()
                .set_op_code("mathCeil")
                .set_type("d")
                .set_spec("ceil %d")
                .set_code("Math.ceil(%s)")
                .set_color(Some(0xFF23B9A9)),
            "mathFloor" => BlockBean::new()
                .set_op_code("mathFloor")
                .set_type("d")
                .set_spec("floor %d")
                .set_code("Math.floor(%s)")
                .set_color(Some(0xFF23B9A9)),
            "mathSin" => BlockBean::new()
                .set_op_code("mathSin")
                .set_type("d")
                .set_spec("sin %d")
                .set_code("Math.sin(%s)")
                .set_color(Some(0xFF23B9A9)),
            "mathCos" => BlockBean::new()
                .set_op_code("mathCos")
                .set_type("d")
                .set_spec("cos %d")
                .set_code("Math.cos(%s)")
                .set_color(Some(0xFF23B9A9)),
            "mathTan" => BlockBean::new()
                .set_op_code("mathTan")
                .set_type("d")
                .set_spec("tan %d")
                .set_code("Math.tan(%s)")
                .set_color(Some(0xFF23B9A9)),
            "mathPow" => BlockBean::new()
                .set_op_code("mathPow")
                .set_type("d")
                .set_spec("%d ^ %d")
                .set_code("Math.pow(%s, %s)")
                .set_color(Some(0xFF23B9A9)),
            "currentTime" => BlockBean::new()
                .set_op_code("currentTime")
                .set_type("d")
                .set_spec("current time (ms)")
                .set_code("System.currentTimeMillis()")
                .set_color(Some(0xFF5CB722)),
            _ => FallbackStatement::get_block(""),
        }
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let name_node = match node.child_by_field_name("name") {
            Some(n) => n,
            None => {
                FallbackStatement::handle(node, generator_coordinator, dispatch_type);
                return;
            }
        };

        let method_name = generator_coordinator.get_code(&name_node).trim().to_string();
        let object_node = node.child_by_field_name("object");
        let object_str = object_node
            .map(|o| generator_coordinator.get_code(&o).trim().to_string())
            .unwrap_or_default();

        let mut args: Vec<Node> = Vec::new();
        if let Some(arg_list) = node.child_by_field_name("arguments") {
            let mut cursor = arg_list.walk();
            for child in arg_list.children(&mut cursor) {
                if child.is_named() {
                    args.push(child);
                }
            }
        }

        match method_name.as_str() {
            "showMessage" | "makeText" => {
                let bean = Self::get_block("doToast");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                let text_node = if args.len() >= 2 { args.get(1) } else { args.first() };
                if let Some(arg) = text_node {
                    generator_coordinator.dispatch_as_block_param(*arg, bean_id, DispatchType::BlockParam("s"));
                }
            }
            "finish" | "finishActivity" => {
                let bean = Self::get_block("finishActivity");
                generator_coordinator.push(bean, dispatch_type);
            }
            "setText" if !args.is_empty() => {
                let bean = Self::get_block("setText");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
            }
            "getText" => {
                let bean = Self::get_block("getText");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
            }
            "setVisibility" if !args.is_empty() => {
                let bean = Self::get_block("setVisible");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                let vis_code = generator_coordinator.get_code(&args[0]).trim().to_string();
                let vis_val = if vis_code.contains("GONE") {
                    "GONE"
                } else if vis_code.contains("INVISIBLE") {
                    "INVISIBLE"
                } else {
                    "VISIBLE"
                };
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(vis_val.to_string());
                });
            }
            "setEnabled" if !args.is_empty() => {
                let bean = Self::get_block("setEnable");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("b"));
            }
            "isEnabled" => {
                let bean = Self::get_block("getEnable");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
            }
            "setChecked" if !args.is_empty() => {
                let bean = Self::get_block("setChecked");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("b"));
            }
            "isChecked" => {
                let bean = Self::get_block("getChecked");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
            }
            "setRotation" if !args.is_empty() => {
                let bean = Self::get_block("setRotate");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "setAlpha" if !args.is_empty() => {
                let bean = Self::get_block("setAlpha");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "setTextColor" if !args.is_empty() => {
                let bean = Self::get_block("setTextColor");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "setBackgroundColor" if !args.is_empty() => {
                let bean = Self::get_block("setBgColor");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "setImageResource" if !args.is_empty() => {
                let bean = Self::get_block("setImage");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                let res_code = generator_coordinator.get_code(&args[0]).trim().replace("R.drawable.", "");
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(res_code);
                });
            }
            "setClass" if args.len() >= 2 => {
                let bean = Self::get_block("intentSetScreen");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                let act_code = generator_coordinator.get_code(&args[1]).trim().replace(".class", "");
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(act_code);
                });
            }
            "startActivity" if !args.is_empty() => {
                let bean = Self::get_block("startActivity");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                let intent_code = generator_coordinator.get_code(&args[0]).trim().to_string();
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(intent_code);
                });
            }
            "putExtra" if args.len() >= 2 => {
                let bean = Self::get_block("intentPutExtra");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
                generator_coordinator.dispatch_as_block_param(args[1], bean_id, DispatchType::BlockParam("s"));
            }
            "setAction" if !args.is_empty() => {
                let bean = Self::get_block("intentSetAction");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
            }
            "equals" if !args.is_empty() => {
                let bean = Self::get_block("stringEquals");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
            }
            "contains" if !args.is_empty() => {
                let bean = Self::get_block("stringContains");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
            }
            "concat" if !args.is_empty() => {
                let bean = Self::get_block("stringJoin");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
            }
            "length" => {
                let bean = Self::get_block("stringLength");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
            }
            "indexOf" if !args.is_empty() => {
                let bean = Self::get_block("stringIndex");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
            }
            "lastIndexOf" if !args.is_empty() => {
                let bean = Self::get_block("stringLastIndex");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
            }
            "substring" if args.len() >= 2 => {
                let bean = Self::get_block("stringSub");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
                generator_coordinator.dispatch_as_block_param(args[1], bean_id, DispatchType::BlockParam("d"));
            }
            "trim" => {
                let bean = Self::get_block("trim");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
            }
            "toUpperCase" => {
                let bean = Self::get_block("toUpperCase");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
            }
            "toLowerCase" => {
                let bean = Self::get_block("toLowerCase");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
            }
            "replace" if args.len() >= 2 => {
                let bean = Self::get_block("stringReplace");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
                generator_coordinator.dispatch_as_block_param(args[1], bean_id, DispatchType::BlockParam("s"));
            }
            "replaceFirst" if args.len() >= 2 => {
                let bean = Self::get_block("stringReplaceFirst");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
                generator_coordinator.dispatch_as_block_param(args[1], bean_id, DispatchType::BlockParam("s"));
            }
            "replaceAll" if args.len() >= 2 => {
                let bean = Self::get_block("stringReplaceAll");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                if let Some(obj) = object_node {
                    generator_coordinator.dispatch_as_block_param(obj, bean_id, DispatchType::BlockParam("s"));
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| b.parameters.push("".to_string()));
                }
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
                generator_coordinator.dispatch_as_block_param(args[1], bean_id, DispatchType::BlockParam("s"));
            }
            "put" if args.len() >= 2 => {
                let bean = Self::get_block("mapPut");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
                generator_coordinator.dispatch_as_block_param(args[1], bean_id, DispatchType::BlockParam("s"));
            }
            "get" if !args.is_empty() => {
                let bean = Self::get_block("mapGet");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
            }
            "remove" if !args.is_empty() => {
                let bean = Self::get_block("mapRemoveKey");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
            }
            "containsKey" if !args.is_empty() => {
                let bean = Self::get_block("mapContainKey");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
            }
            "add" if !args.is_empty() => {
                let bean = Self::get_block("addListStr");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("s"));
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
            }
            "clear" => {
                let bean = Self::get_block("clearList");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
            }
            "size" => {
                let bean = Self::get_block("lengthList");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push(object_str);
                });
            }
            "getRandom" if args.len() >= 2 => {
                let bean = Self::get_block("random");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
                generator_coordinator.dispatch_as_block_param(args[1], bean_id, DispatchType::BlockParam("d"));
            }
            "min" if args.len() >= 2 => {
                let bean = Self::get_block("mathMin");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
                generator_coordinator.dispatch_as_block_param(args[1], bean_id, DispatchType::BlockParam("d"));
            }
            "max" if args.len() >= 2 => {
                let bean = Self::get_block("mathMax");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
                generator_coordinator.dispatch_as_block_param(args[1], bean_id, DispatchType::BlockParam("d"));
            }
            "abs" if !args.is_empty() => {
                let bean = Self::get_block("mathAbs");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "sqrt" if !args.is_empty() => {
                let bean = Self::get_block("mathSqrt");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "round" if !args.is_empty() => {
                let bean = Self::get_block("mathRound");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "ceil" if !args.is_empty() => {
                let bean = Self::get_block("mathCeil");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "floor" if !args.is_empty() => {
                let bean = Self::get_block("mathFloor");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "sin" if !args.is_empty() => {
                let bean = Self::get_block("mathSin");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "cos" if !args.is_empty() => {
                let bean = Self::get_block("mathCos");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "tan" if !args.is_empty() => {
                let bean = Self::get_block("mathTan");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
            }
            "pow" if args.len() >= 2 => {
                let bean = Self::get_block("mathPow");
                let bean_id = generator_coordinator.push(bean, dispatch_type);
                generator_coordinator.dispatch_as_block_param(args[0], bean_id, DispatchType::BlockParam("d"));
                generator_coordinator.dispatch_as_block_param(args[1], bean_id, DispatchType::BlockParam("d"));
            }
            "currentTimeMillis" => {
                let bean = Self::get_block("currentTime");
                generator_coordinator.push(bean, dispatch_type);
            }
            _ => {
                FallbackStatement::handle(node, generator_coordinator, dispatch_type);
            }
        }
    }
}
