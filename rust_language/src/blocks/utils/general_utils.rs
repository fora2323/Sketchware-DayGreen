use crate::blocks::block_palette::BlockBean;
use rustc_hash::FxHashMap;
use std::collections::HashSet;

#[allow(unused)]
pub fn pretty_print_blocks(blocks: &Vec<BlockBean>) {
    if blocks.is_empty() {
        return;
    }

    let mut map: FxHashMap<i32, &BlockBean> = FxHashMap::default();
    let mut referenced: HashSet<i32> = HashSet::new();

    for b in blocks {
        let id = b.id;
        map.insert(id, b);

        if b.next_block != -1 {
            referenced.insert(b.next_block);
        }
        if b.sub_stack1 != -1 {
            referenced.insert(b.sub_stack1);
        }
        if b.sub_stack2 != -1 {
            referenced.insert(b.sub_stack2);
        }
    }

    let roots: Vec<i32> = map
        .keys()
        .filter(|id| !referenced.contains(id))
        .cloned()
        .collect();

    let mut visited = HashSet::new();

    for root in roots {
        print_block(root, &map, &mut visited, "", "");
    }
}
#[allow(unused)]
fn print_block(
    id: i32,
    map: &FxHashMap<i32, &BlockBean>,
    visited: &mut HashSet<i32>,
    indent: &str,
    branch: &str,
) {
    if visited.contains(&id) {
        return;
    }

    let block = match map.get(&id) {
        Some(b) => *b,
        None => return,
    };

    visited.insert(id);

    println!(
        "{}{}Block {} (op_code: {}, next: {}, sub1: {}, sub2: {})",
        indent,
        branch,
        block.id,
        block.op_code,
        block.next_block,
        block.sub_stack1,
        block.sub_stack2
    );

    let next_indent = format!("{}{}", indent, if branch.is_empty() { "" } else { "│  " });

    if block.sub_stack1 != -1 {
        print_block(block.sub_stack1, map, visited, &next_indent, "|-- ");
    }

    if block.sub_stack2 != -1 {
        print_block(block.sub_stack2, map, visited, &next_indent, "|== ");
    }

    if block.next_block != -1 {
        print_block(block.next_block, map, visited, indent, branch);
    }
}
