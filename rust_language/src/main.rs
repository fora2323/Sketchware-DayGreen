use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;
use std::time::Instant;

mod blocks;

fn main() {
    let alloc_t = Instant::now();
    let init_code = r#"
    if ((!true || false) && (true && !false) || (i++ > 0 ? true : false) || (~i < 0) && (++i != i)) {
    // hi
    } else if (!false) {
    // bye
    }
"#;
    let repeats = 50_000;

    let capacity = init_code.len() * repeats;
    let mut big_code = String::with_capacity(capacity);

    for _ in 0..repeats {
        big_code.push_str(init_code);
    }

    println!("allocating the code took : {:?}", alloc_t.elapsed());
    println!("starting the generation......");

    let timer = Instant::now();

    let generator = JavaBlocksGenerator::new(big_code);

    let blocks = generator.generate().unwrap();

    println!("Generated {} blocks in {:?}", blocks.len(), timer.elapsed());
}
