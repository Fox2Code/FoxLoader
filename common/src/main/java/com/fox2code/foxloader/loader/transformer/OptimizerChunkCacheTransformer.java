package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class OptimizerChunkCacheTransformer implements PreClassTransformer {
    private static final boolean DEBUG = true;
    private static final String CHUNK_CACHE = "net/minecraft/src/game/level/chunk/ChunkCache";
    private static final String CHUNK = "net/minecraft/src/game/level/chunk/Chunk";
    private static final String[] chunkFields = new String[]{"chunkX", "chunkY", "chunkZ"};
    private static final int[] opcodeAssumption = new int[]{
            ILOAD, ICONST_4, ISHR, ALOAD, GETFIELD, ISUB, ISTORE
    };
    private static final int[] opcodeAssumption2 = new int[]{
            GETFIELD, ILOAD, AALOAD, ILOAD, AALOAD, ILOAD, AALOAD
    };

    @Override
    public void transform(ClassNode classNode, String className) {
        if (!"net.minecraft.src.game.level.chunk.ChunkCache".equals(className)) return;

        classNode.fields.add(new FieldNode(ACC_PRIVATE, "lastChunkX", "I", null, null));
        classNode.fields.add(new FieldNode(ACC_PRIVATE, "lastChunkY", "I", null, null));
        classNode.fields.add(new FieldNode(ACC_PRIVATE, "lastChunkZ", "I", null, null));
        classNode.fields.add(new FieldNode(ACC_PRIVATE, "lastChunk", "L" + CHUNK + ";", null, null));
        appendInitializer(TransformerUtils.getMethod(classNode, "<init>"));
        MethodNode set = TransformerUtils.findMethod(classNode, "set");
        if (set != null) {
            appendInitializer(set);
        }
        for (MethodNode methodNode : classNode.methods) {
            if (!methodNode.name.startsWith("<") &&
                    !methodNode.name.equals("set") &&
                    !methodNode.name.equals("reset")) {
                optimizeSingleMethod(methodNode);
            }
        }
    }

    public void appendInitializer(MethodNode methodNode) {
        // Prefill cache with one chunk to avoid having to handle edge cases
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new InsnNode(ICONST_0));
        insnList.add(new FieldInsnNode(PUTFIELD, CHUNK_CACHE, "lastChunkX", "I"));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new InsnNode(ICONST_0));
        insnList.add(new FieldInsnNode(PUTFIELD, CHUNK_CACHE, "lastChunkY", "I"));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new InsnNode(ICONST_0));
        insnList.add(new FieldInsnNode(PUTFIELD, CHUNK_CACHE, "lastChunkZ", "I"));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new FieldInsnNode(GETFIELD, CHUNK_CACHE, "chunkArray", "[[[L" + CHUNK + ";"));
        insnList.add(new InsnNode(ICONST_0));
        insnList.add(new InsnNode(AALOAD));
        insnList.add(new InsnNode(ICONST_0));
        insnList.add(new InsnNode(AALOAD));
        insnList.add(new InsnNode(ICONST_0));
        insnList.add(new InsnNode(AALOAD));
        insnList.add(new FieldInsnNode(PUTFIELD, CHUNK_CACHE, "lastChunk", "L" + CHUNK + ";"));
        AbstractInsnNode abstractInsnNode = methodNode.instructions.getLast();
        while (abstractInsnNode != null && abstractInsnNode.getOpcode() != RETURN) {
            abstractInsnNode = abstractInsnNode.getPrevious();
        }
        if (abstractInsnNode == null) {
            throw new RuntimeException("Incompatible return type for \"set" + methodNode.desc + "\"???");
        }
        methodNode.instructions.insertBefore(abstractInsnNode, insnList);
    }

    public void optimizeSingleMethod(MethodNode methodNode) {
        String debugName = methodNode.name + methodNode.desc;
        final int returnOpcode = Type.getReturnType(methodNode.desc).getOpcode(IRETURN);
        InsnList methodInsns = methodNode.instructions;
        AbstractInsnNode opcode = methodInsns.getFirst();
        int assumptionX = 0, assumptionY = 0, assumptionZ = 0;
        assumptionLoop:
        for (int i = 0; i < 3; i++) {
            String fieldName = chunkFields[i];
            for (int assumedOpcode : opcodeAssumption) {
                while (opcode.getOpcode() == -1) {
                    opcode = opcode.getNext();
                    if (opcode == null)
                        break assumptionLoop;
                }
                if (opcode.getOpcode() != assumedOpcode) {
                    if (DEBUG) {
                        System.out.println("Failed assumption of " + debugName + " cause wrong opcode, expected: " +
                                assumedOpcode + ", got " + opcode.getOpcode());
                    }
                    break assumptionLoop;
                }
                if (opcode.getOpcode() == GETFIELD &&
                        !fieldName.equals(((FieldInsnNode) opcode).name)) {
                    if (DEBUG) {
                        System.out.println("Failed assumption of " + debugName + " cause wrong get field name, expected: " +
                                fieldName + ", got " + ((FieldInsnNode) opcode).name);
                    }
                    break assumptionLoop;
                }
                if (opcode.getOpcode() == ISTORE) {
                    switch (i) {
                        case 0:
                            assumptionX = ((VarInsnNode) opcode).var;
                            break;
                        case 1:
                            assumptionY = ((VarInsnNode) opcode).var;
                            break;
                        case 2:
                            assumptionZ = ((VarInsnNode) opcode).var;
                            if (DEBUG) {
                                System.out.println("Assumed " +
                                        methodNode.name + methodNode.desc);
                            }
                            break;
                    }
                }
                opcode = opcode.getNext();
                if (opcode == null)
                    break assumptionLoop;
            }
        }
        // If we can, try using header assumption
        if (assumptionZ != 0 && opcode != null) {
            AbstractInsnNode retOpCode = opcode.getNext();
            while (retOpCode.getOpcode() != returnOpcode) {
                retOpCode = retOpCode.getNext();
                if (retOpCode == null) break;
            }
            if (retOpCode != null) {
                while (retOpCode.getOpcode() != AALOAD) {
                    retOpCode = retOpCode.getPrevious();
                    if (retOpCode == null) break;
                }
                if (retOpCode != null) {
                    retOpCode = retOpCode.getNext();
                    if (retOpCode.getOpcode() == ASTORE) {
                        VarInsnNode aStore = (VarInsnNode) retOpCode;
                        LabelNode next;
                        if (retOpCode.getNext() instanceof LabelNode) {
                            next = (LabelNode) retOpCode.getNext();
                        } else {
                            methodInsns.insert(retOpCode, next = new LabelNode());
                        }
                        InsnList toInjectPre = new InsnList();
                        LabelNode fallback = new LabelNode();
                        if (methodNode.localVariables != null) {
                            for (LocalVariableNode localVariableNode : methodNode.localVariables) {
                                if (localVariableNode.index == aStore.var) {
                                    toInjectPre.add(localVariableNode.start = new LabelNode());
                                }
                            }
                        }
                        toInjectPre.add(new VarInsnNode(ALOAD, 0));
                        toInjectPre.add(new FieldInsnNode(GETFIELD, CHUNK_CACHE, "lastChunkX", "I"));
                        toInjectPre.add(new VarInsnNode(ILOAD, assumptionX));
                        toInjectPre.add(new JumpInsnNode(IF_ICMPNE, fallback));
                        toInjectPre.add(new VarInsnNode(ALOAD, 0));
                        toInjectPre.add(new FieldInsnNode(GETFIELD, CHUNK_CACHE, "lastChunkY", "I"));
                        toInjectPre.add(new VarInsnNode(ILOAD, assumptionY));
                        toInjectPre.add(new JumpInsnNode(IF_ICMPNE, fallback));
                        toInjectPre.add(new VarInsnNode(ALOAD, 0));
                        toInjectPre.add(new FieldInsnNode(GETFIELD, CHUNK_CACHE, "lastChunkZ", "I"));
                        toInjectPre.add(new VarInsnNode(ILOAD, assumptionZ));
                        toInjectPre.add(new JumpInsnNode(IF_ICMPNE, fallback));
                        toInjectPre.add(new VarInsnNode(ALOAD, 0));
                        toInjectPre.add(new FieldInsnNode(GETFIELD, CHUNK_CACHE, "lastChunk", "L" + CHUNK + ";"));
                        toInjectPre.add(new VarInsnNode(ASTORE, aStore.var));
                        // Work like "toInjectPre.add(new JumpInsnNode(GOTO, next));" but
                        // work better with decompilers to produce beginner-friendly code.
                        toInjectPre.add(TransformerUtils.copyCodeUntil(next, returnOpcode));
                        toInjectPre.add(fallback);

                        InsnList toInjectPost = new InsnList();
                        toInjectPost.add(new VarInsnNode(ALOAD, 0));
                        toInjectPost.add(new VarInsnNode(ILOAD, assumptionX));
                        toInjectPost.add(new FieldInsnNode(PUTFIELD, CHUNK_CACHE, "lastChunkX", "I"));
                        toInjectPost.add(new VarInsnNode(ALOAD, 0));
                        toInjectPost.add(new VarInsnNode(ILOAD, assumptionY));
                        toInjectPost.add(new FieldInsnNode(PUTFIELD, CHUNK_CACHE, "lastChunkY", "I"));
                        toInjectPost.add(new VarInsnNode(ALOAD, 0));
                        toInjectPost.add(new VarInsnNode(ILOAD, assumptionZ));
                        toInjectPost.add(new FieldInsnNode(PUTFIELD, CHUNK_CACHE, "lastChunkZ", "I"));
                        toInjectPost.add(new VarInsnNode(ALOAD, 0));
                        toInjectPost.add(new VarInsnNode(ALOAD, aStore.var));
                        toInjectPost.add(new FieldInsnNode(PUTFIELD, CHUNK_CACHE, "lastChunk", "L" + CHUNK + ";"));
                        // Do the injection
                        methodInsns.insert(opcode, toInjectPre);
                        methodInsns.insertBefore(next, toInjectPost);

                        if (DEBUG) {
                            System.out.println("Injected into " + debugName + " with the method hook way!");
                        }
                        return;
                    }
                }
            }
        }
        // Try different assumption set then
        opcode = null;
        assumptionZ = 0;
        FieldInsnNode fieldInsnNode = null;
        assumptionLoop:
        for (AbstractInsnNode abstractInsnNode : methodInsns) {
            if (abstractInsnNode.getOpcode() == GETFIELD &&
                    (fieldInsnNode = (FieldInsnNode) abstractInsnNode).name.equals("chunkArray")) {
                int i = 0;

                for (int assumedOpcode : opcodeAssumption2) {
                    while (abstractInsnNode.getOpcode() == -1) {
                        abstractInsnNode = abstractInsnNode.getNext();
                        if (abstractInsnNode == null) {
                            assumptionZ = 0;
                            continue assumptionLoop;
                        }
                    }
                    if (abstractInsnNode.getOpcode() != assumedOpcode) {
                        if (DEBUG) {
                            System.out.println("Failed assumption of a " + debugName +
                                    " chunkArray field instruction cause wrong opcode, expected: " +
                                    assumedOpcode + ", got " + abstractInsnNode.getOpcode());
                        }
                        assumptionZ = 0;
                        continue assumptionLoop;
                    }
                    if (assumedOpcode == ILOAD) {
                        switch (i++) {
                            case 0:
                                assumptionX = ((VarInsnNode) abstractInsnNode).var;
                                break;
                            case 1:
                                assumptionY = ((VarInsnNode) abstractInsnNode).var;
                                break;
                            case 2:
                                assumptionZ = ((VarInsnNode) abstractInsnNode).var;
                                break;
                        }
                    } else if (assumedOpcode == AALOAD && assumptionZ != 0) {
                        opcode = abstractInsnNode;
                        if (DEBUG) {
                            System.out.println("Assumed for instruction " +
                                    methodNode.name + methodNode.desc);
                        }
                        break assumptionLoop;
                    }
                    abstractInsnNode = abstractInsnNode.getNext();
                    if (abstractInsnNode == null) {
                        continue assumptionLoop;
                    }
                }
            }
        }
        if (assumptionZ != 0 && opcode != null &&
                fieldInsnNode.getPrevious().getOpcode() == ALOAD) {
            InsnList toInjectPre = new InsnList();
            LabelNode fallback = new LabelNode();
            toInjectPre.add(new VarInsnNode(ALOAD, 0));
            toInjectPre.add(new FieldInsnNode(GETFIELD, CHUNK_CACHE, "lastChunkX", "I"));
            toInjectPre.add(new VarInsnNode(ILOAD, assumptionX));
            toInjectPre.add(new JumpInsnNode(IF_ICMPNE, fallback));
            toInjectPre.add(new VarInsnNode(ALOAD, 0));
            toInjectPre.add(new FieldInsnNode(GETFIELD, CHUNK_CACHE, "lastChunkY", "I"));
            toInjectPre.add(new VarInsnNode(ILOAD, assumptionY));
            toInjectPre.add(new JumpInsnNode(IF_ICMPNE, fallback));
            toInjectPre.add(new VarInsnNode(ALOAD, 0));
            toInjectPre.add(new FieldInsnNode(GETFIELD, CHUNK_CACHE, "lastChunkZ", "I"));
            toInjectPre.add(new VarInsnNode(ILOAD, assumptionZ));
            toInjectPre.add(new JumpInsnNode(IF_ICMPNE, fallback));
            toInjectPre.add(new VarInsnNode(ALOAD, 0));
            toInjectPre.add(new FieldInsnNode(GETFIELD, CHUNK_CACHE, "lastChunk", "L" + CHUNK + ";"));
            LabelNode solved = new LabelNode();
            toInjectPre.add(new JumpInsnNode(GOTO, solved));
            toInjectPre.add(fallback);
            methodInsns.insertBefore(fieldInsnNode.getPrevious(), toInjectPre);
            methodInsns.insert(opcode, solved);
            if (DEBUG) {
                System.out.println("Injected into " + debugName + " with the instruction hook way!");
            }
        } else if (DEBUG && assumptionZ != 0) {
            System.out.println(assumptionZ + " " +
                    (opcode == null ? "null" : opcode.getOpcode()) + " " +
                    fieldInsnNode.name + " " + fieldInsnNode.getPrevious().getOpcode());
        }
    }
}
