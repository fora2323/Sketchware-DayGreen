use crate::blocks::block_palette::{BlockBean, BlockPalette};

pub struct Operators;

impl Operators {
    pub fn load() -> BlockPalette {
        let mut block_palette = BlockPalette::new("Operators", 0xFF5CB722);

        block_palette.push(
            BlockBean::new()
                .set_header_text("Literal Values")
                .set_op_code("true")
                .set_type("b")
                .set_code("true")
                .set_spec("true"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("false")
                .set_type("b")
                .set_code("false")
                .set_spec("false"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("null_literal")
                .set_type("v.Object")
                .set_code("null")
                .set_spec("null pointer"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Parenthesized expression")
                .set_op_code("parenthesized_expression")
                .set_type("b")
                .set_code("(%s)")
                .set_spec("( %b )"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Comparison Operators")
                .set_op_code(">")
                .set_type("b")
                .set_code("%s > %s")
                .set_spec("%d > %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("<")
                .set_type("b")
                .set_code("%s < %s")
                .set_spec("%d < %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("==")
                .set_type("b")
                .set_code("%s == %s")
                .set_spec("%d == %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("!=")
                .set_type("b")
                .set_code("%s != %s")
                .set_spec("%d != %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code(">=")
                .set_type("b")
                .set_code("%s >= %s")
                .set_spec("%d >= %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("<=")
                .set_type("b")
                .set_code("%s <= %s")
                .set_spec("%d <= %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Logical Operators")
                .set_op_code("&&")
                .set_type("b")
                .set_code("%s && %s")
                .set_spec("%b and %b"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("||")
                .set_type("b")
                .set_code("%s || %s")
                .set_spec("%b or %b"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("instanceof_expression")
                .set_type("b")
                .set_code("%s instanceof %s")
                .set_spec("%m.Object instanceof %m.Type"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Casting expression")
                .set_op_code("cast_expression")
                .set_type("b")
                .set_code("(%s) %s")
                .set_spec("( %m.Type ) %m.Object"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Math Operators")
                .set_op_code("+")
                .set_type("d")
                .set_code("%s + %s")
                .set_spec("%d + %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("-")
                .set_type("d")
                .set_code("%s - %s")
                .set_spec("%d - %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("*")
                .set_type("d")
                .set_code("%s * %s")
                .set_spec("%d * %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("/")
                .set_type("d")
                .set_code("%s / %s")
                .set_spec("%d / %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("%")
                .set_type("d")
                .set_code("%s % %s")
                .set_spec("%d % %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Unary expressions")
                .set_op_code("!")
                .set_type("b")
                .set_code("!%s")
                .set_spec("! %b"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("-")
                .set_type("d")
                .set_code("-%s")
                .set_spec("- %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("+")
                .set_type("d")
                .set_code("+%s")
                .set_spec("+ %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("~")
                .set_type("d")
                .set_code("~%s")
                .set_spec("~ %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Update expression")
                .set_op_code("++x")
                .set_type("d")
                .set_code("++%s")
                .set_spec("++ %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("x++")
                .set_type("d")
                .set_code("%s++")
                .set_spec("%d ++"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("--x")
                .set_type("d")
                .set_code("--%s")
                .set_spec("-- %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("x--")
                .set_type("d")
                .set_code("%s--")
                .set_spec("%d --"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Ternary expression")
                .set_op_code("ternary_expression")
                .set_type("s")
                .set_code("%s ? %s : %s")
                .set_spec("%b ? %s : %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Binary Manipulation Operators")
                .set_op_code("&")
                .set_type("d")
                .set_code("%s & %s")
                .set_spec("%d & %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("|")
                .set_type("d")
                .set_code("%s | %s")
                .set_spec("%d | %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("^")
                .set_type("d")
                .set_code("%s ^ %s")
                .set_spec("%d ^ %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("<<")
                .set_type("d")
                .set_code("%s << %s")
                .set_spec("%d << %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code(">>")
                .set_type("d")
                .set_code("%s >> %s")
                .set_spec("%d >> %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code(">>>")
                .set_type("d")
                .set_code("%s >>> %s")
                .set_spec("%d >>> %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("hex_integer_literal")
                .set_type("d")
                .set_code("%s")
                .set_spec("from hex int %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("hex_floating_point_literal")
                .set_type("d")
                .set_code("%s")
                .set_spec("from hex float %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("binary_integer_literal")
                .set_type("d")
                .set_code("%s")
                .set_spec("from binary %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("String Comparison Operators")
                .set_op_code("equals")
                .set_type("b")
                .set_code("%s.equals(%s)")
                .set_spec("%s equals %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("equalsIgnoreCase")
                .set_type("b")
                .set_code("%s.equalsIgnoreCase(%s)")
                .set_spec("%s equalsIgnoreCase %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("isEmpty")
                .set_type("b")
                .set_code("%s.isEmpty()")
                .set_spec("%s is empty"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("contains")
                .set_type("b")
                .set_code("%s.contains(%s)")
                .set_spec("%s contains %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("startsWith")
                .set_type("b")
                .set_code("%s.startsWith(%s)")
                .set_spec("%s startsWith %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("endsWith")
                .set_type("b")
                .set_code("%s.endsWith(%s)")
                .set_spec("%s endsWith %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("matches")
                .set_type("b")
                .set_code("%s.matches(%s)")
                .set_spec("%s matches %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("String Manipulation Operators")
                .set_op_code("length")
                .set_type("d")
                .set_code("%s.length()")
                .set_spec("length of %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("indexOf")
                .set_type("d")
                .set_code("%s.indexOf(%s)")
                .set_spec("find in %s the index of %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("lastIndexOf")
                .set_type("d")
                .set_code("%s.lastIndexOf(%s)")
                .set_spec("find in %s the last index of %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("substring")
                .set_type("s")
                .set_code("%s.substring(%s, %s)")
                .set_spec("substring of %s from %d to %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("substring_from")
                .set_type("s")
                .set_code("%s.substring(%s)")
                .set_spec("substring of %s from %d"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("replace")
                .set_type("s")
                .set_code("%s.replace(%s, %s)")
                .set_spec("in %s replace %s with %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("replaceAll")
                .set_type("s")
                .set_code("%s.replaceAll(%s, %s")
                .set_spec("in %s replace all %s with %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("concat")
                .set_type("s")
                .set_code("%s.concat(%s)")
                .set_spec("join %s with %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("trim")
                .set_type("s")
                .set_code("%s.trim()")
                .set_spec("trim %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("fromHtml")
                .set_type("s")
                .set_code("android.text.Html.fromHtml(%s).toString()")
                .set_spec("from html %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("toLowerCase")
                .set_type("s")
                .set_code("%s.toLowerCase()")
                .set_spec("to lower case %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("toUpperCase")
                .set_type("s")
                .set_code("%s.toUpperCase()")
                .set_spec("to upper case %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("reverse")
                .set_type("s")
                .set_code("new StringBuilder(%s).reverse().toString()")
                .set_spec("reverse %s"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("toString")
                .set_type("s")
                .set_code("String.valueOf(%s)")
                .set_spec("%m.Object to string"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("add source directly blocks")
                .set_op_code("addSourceDirectly")
                .set_type(" ")
                .set_code("%s")
                .set_spec("ASD regular %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("addSourceDirectlyString")
                .set_type("s")
                .set_code("%s")
                .set_spec("ASD string %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("addSourceDirectlyDouble")
                .set_type("d")
                .set_code("%s")
                .set_spec("ASD number %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("addSourceDirectlyBool")
                .set_type("b")
                .set_code("%s")
                .set_spec("ASD boolean %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("addSourceDirectlyObject")
                .set_type("v.Object")
                .set_code("%s")
                .set_spec("ASD Object %asd"),
        );

        block_palette
    }
}
