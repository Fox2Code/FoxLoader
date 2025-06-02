package com.fox2code.foxloader.patching.dev;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Textifier;

import java.awt.*;
import java.util.*;

public final class DevelopmentSourceTransformer implements Opcodes {
    private static final HashMap<String, ConstantUnpick> staticConstantUnpicks = new HashMap<>();
    private static final HashMap<String, ConstantUnpick> virtualConstantUnpicks = new HashMap<>();
    private static final HashMap<String, ConstantUnpick> returnStaticConstantUnpicks = new HashMap<>();
    private static final HashMap<String, ConstantUnpick> putStaticConstantUnpicks = new HashMap<>();
    private static final HashMap<String, ConstantUnpick> putVirtualConstantUnpicks = new HashMap<>();

    static {
        // Java Unpicks
        ConstantUnpick longExtremesUnpicks = new LongStaticConstantUnpick("java/lang/Long") {
            @Override
            public String unpick(long value) {
                if (value == Long.MIN_VALUE) {
                    return "MIN_VALUE";
                } else if (value == Long.MAX_VALUE) {
                    return "MAX_VALUE";
                }
                return null;
            }
        };
        ConstantUnpick threadPriorityUnpick = new IntStaticConstantUnpick("java/lang/Thread") {
            @Override
            public String unpick(int value) {
                switch (value) {
                    case Thread.MIN_PRIORITY:
                        return "MIN_PRIORITY";
                    case Thread.NORM_PRIORITY:
                        return "NORM_PRIORITY";
                    case Thread.MAX_PRIORITY:
                        return "MAX_PRIORITY";
                    default:
                        return null;
                }
            }
        };
        virtualConstantUnpicks.put("java/lang/Thread.setPriority(I)V", threadPriorityUnpick);
        staticConstantUnpicks.put("java/lang/Thread.sleep(J)V", longExtremesUnpicks);
        // AWT/Swing Unpicks
        ConstantUnpick borderLayoutUnpick = new StringConstantUnpick() {
            @Override
            public void unpick(InsnList insnList, AbstractInsnNode constant, String value) {
                switch (value) {
                    case BorderLayout.CENTER:
                        insnList.insert(constant, new FieldInsnNode(Opcodes.GETSTATIC, "java/awt/BorderLayout", "CENTER", "I"));
                        insnList.remove(constant);
                        break;
                    case BorderLayout.NORTH:
                        insnList.insert(constant, new FieldInsnNode(Opcodes.GETSTATIC, "java/awt/BorderLayout", "NORTH", "I"));
                        insnList.remove(constant);
                        break;
                    case BorderLayout.SOUTH:
                        insnList.insert(constant, new FieldInsnNode(Opcodes.GETSTATIC, "java/awt/BorderLayout", "SOUTH", "I"));
                        insnList.remove(constant);
                        break;
                    case BorderLayout.EAST:
                        insnList.insert(constant, new FieldInsnNode(Opcodes.GETSTATIC, "java/awt/BorderLayout", "EAST", "I"));
                        insnList.remove(constant);
                        break;
                    case BorderLayout.WEST:
                        insnList.insert(constant, new FieldInsnNode(Opcodes.GETSTATIC, "java/awt/BorderLayout", "WEST", "I"));
                        insnList.remove(constant);
                        break;
                }
            }
        };
        virtualConstantUnpicks.put("java/awt/Frame.add(Ljava/awt/Component;Ljava/lang/Object;)V", borderLayoutUnpick);
        virtualConstantUnpicks.put("javax/swing/JPanel.add(Ljava/awt/Component;Ljava/lang/Object;)V", borderLayoutUnpick);
        // Minecraft specific AWT/Swing Unpicks
        virtualConstantUnpicks.put("net/minecraft/client/MinecraftApplet.add(Ljava/awt/Component;Ljava/lang/Object;)V", borderLayoutUnpick);
        virtualConstantUnpicks.put("net/minecraft/server/gui/ServerGUI.add(Ljava/awt/Component;Ljava/lang/Object;)V", borderLayoutUnpick);
        // LWJGL/OpenGL Unpicks
        ConstantUnpick glAttribBits = new FlagIntStaticConstantUnpick("org/lwjgl/opengl/GL11",
                new Flag(GL11.GL_ALL_ATTRIB_BITS, "GL_ALL_ATTRIB_BITS"),
                new Flag(GL11.GL_CURRENT_BIT, "GL_CURRENT_BIT"),
                new Flag(GL11.GL_POINT_BIT, "GL_POINT_BIT"),
                new Flag(GL11.GL_LINE_BIT, "GL_LINE_BIT"),
                new Flag(GL11.GL_POLYGON_BIT, "GL_POLYGON_BIT"),
                new Flag(GL11.GL_POLYGON_STIPPLE_BIT, "GL_POLYGON_STIPPLE_BIT"),
                new Flag(GL11.GL_PIXEL_MODE_BIT, "GL_PIXEL_MODE_BIT"),
                new Flag(GL11.GL_LIGHTING_BIT, "GL_LIGHTING_BIT"),
                new Flag(GL11.GL_FOG_BIT, "GL_FOG_BIT"),
                new Flag(GL11.GL_DEPTH_BUFFER_BIT, "GL_DEPTH_BUFFER_BIT"),
                new Flag(GL11.GL_ACCUM_BUFFER_BIT, "GL_ACCUM_BUFFER_BIT"),
                new Flag(GL11.GL_STENCIL_BUFFER_BIT, "GL_STENCIL_BUFFER_BIT"),
                new Flag(GL11.GL_VIEWPORT_BIT, "GL_VIEWPORT_BIT"),
                new Flag(GL11.GL_TRANSFORM_BIT, "GL_TRANSFORM_BIT"),
                new Flag(GL11.GL_ENABLE_BIT, "GL_ENABLE_BIT"),
                new Flag(GL11.GL_COLOR_BUFFER_BIT, "GL_COLOR_BUFFER_BIT"),
                new Flag(GL11.GL_HINT_BIT, "GL_HINT_BIT"),
                new Flag(GL11.GL_EVAL_BIT, "GL_EVAL_BIT"),
                new Flag(GL11.GL_LIST_BIT, "GL_LIST_BIT"),
                new Flag(GL11.GL_TEXTURE_BIT, "GL_TEXTURE_BIT"),
                new Flag(GL11.GL_SCISSOR_BIT, "GL_SCISSOR_BIT"));
        ConstantUnpick glZeroOne = new IntStaticConstantUnpick("org/lwjgl/opengl/GL11") {
            @Override
            public String unpick(int value) {
                switch (value) {
                    case GL11.GL_ZERO:
                        return "GL_ZERO";
                    case GL11.GL_ONE:
                        return "GL_ONE";
                    default:
                        return null;
                }
            }
        };
        ConstantUnpick glParam = GeneratedConstantUnpicks.openGLConstantUnpick;
        ConstantUnpick glParamNum = new MultiConstantUnpick(glZeroOne, glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glClear(I)V", glAttribBits);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glEnable(I)V", glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glDisable(I)V", glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glEnableClientState(I)V", glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glDisableClientState(I)V", glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glGetString(I)Ljava/lang/String;", glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glBindTexture(II)V",
                new ParamsConstantUnpick(glParam, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glCullFace(I)V", glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glMatrixMode(I)V", glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glDepthFunc(I)V", glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glBlendFunc(II)V",
                new ParamsConstantUnpick(glParamNum, glParamNum));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glAlphaFunc(IF)V",
                new ParamsConstantUnpick(glParamNum, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glShadeModel(I)V", glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glTexImage2D(IIIIIIIILjava/nio/ByteBuffer;)V",
                new ParamsConstantUnpick(glParam, null, glParam, null, null, null, glParam, glParam,
                        new CheckCastConstantUnpick("java/nio/ByteBuffer")));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glTexImage2D(IIIIIIIILjava/nio/IntBuffer;)V",
                new ParamsConstantUnpick(glParam, null, glParam, null, null, null, glParam, glParam,
                        new CheckCastConstantUnpick("java/nio/IntBuffer")));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glTexSubImage2D(IIIIIIIILjava/nio/IntBuffer;)V",
                new ParamsConstantUnpick(glParam, null, null, null, null, null, glParam, glParam,
                        new CheckCastConstantUnpick("java/nio/IntBuffer")));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glTexParameteri(III)V",
                new ParamsConstantUnpick(glParam, glParam, glParam));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glGetTexLevelParameteri(III)I",
                new ParamsConstantUnpick(glParam, glParam, glParam));
        for (String typeName : new String[]{"Float", "Double", "Integer"}) {
            String desc = typeName.substring(0, 1);
            String bufName = typeName.equals("Integer") ? "Int" : typeName;
            staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glGet" +
                            typeName + "(ILjava/nio/" + bufName + "Buffer;)V",
                    new ParamsConstantUnpick(glParam,
                            new CheckCastConstantUnpick("java/nio/" + bufName + "Buffer")));
            staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glGet" +
                    typeName + "(I)" + desc, glParam);
        }
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glColorMaterial(II)V",
                new ParamsConstantUnpick(glParamNum, glParamNum));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glFogi(II)V",
                new ParamsConstantUnpick(glParamNum, glParamNum));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glFogf(IF)V",
                new ParamsConstantUnpick(glParamNum, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glNewList(II)V",
                new ParamsConstantUnpick(null, glParam));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glColorPointer(IIIJ)V",
                new ParamsConstantUnpick(null, glParam, null, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glNormalPointer(IIJ)V",
                new ParamsConstantUnpick(glParam, null, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glVertexPointer(IIIJ)V",
                new ParamsConstantUnpick(null, glParam, null, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glTexCoordPointer(IIIJ)V",
                new ParamsConstantUnpick(null, glParam, null, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glLightModel(ILjava/nio/FloatBuffer;)V",
                new ParamsConstantUnpick(glParam, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glLight(IILjava/nio/FloatBuffer;)V",
                new ParamsConstantUnpick(glParam, glParam, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL11.glFog(ILjava/nio/FloatBuffer;)V",
                new ParamsConstantUnpick(glParam, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL14.glBlendFuncSeparate(IIII)V",
                new ParamsConstantUnpick(glParamNum, glParamNum, glParamNum, glParamNum));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL15.glBindBuffer(II)V",
                new ParamsConstantUnpick(glParam, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/GL15.glBufferData(ILjava/nio/ByteBuffer;I)V",
                new ParamsConstantUnpick(glParam, null, glParam));
        // LWJGL/OpenGL ARB/EXT Unpicks
        ConstantUnpick glParamARBVertexBufferObject = new MultiConstantUnpick(
                GeneratedConstantUnpicks.arbVertexBufferObjectConstantUnpick, glParam);
        staticConstantUnpicks.put("org/lwjgl/opengl/ARBVertexBufferObject.glBindBufferARB(II)V",
                new ParamsConstantUnpick(glParamARBVertexBufferObject, null));
        staticConstantUnpicks.put("org/lwjgl/opengl/ARBVertexBufferObject.glBufferDataARB(ILjava/nio/ByteBuffer;I)V",
                new ParamsConstantUnpick(glParamARBVertexBufferObject, null, glParamARBVertexBufferObject));
        // Indigo3D specific Unpicks
        staticConstantUnpicks.put("com/indigo3d/util/RenderSystem.alphaFunc(IF)V",
                new ParamsConstantUnpick(glParam, null));
        staticConstantUnpicks.put("com/indigo3d/util/RenderSystem.depthFunc(I)V", glParam);
        staticConstantUnpicks.put("com/indigo3d/util/RenderSystem.blendFunc(II)V",
                new ParamsConstantUnpick(glParam, glParam));
        staticConstantUnpicks.put("com/indigo3d/util/RenderSystem.useShadeModel(I)V", glParam);
        staticConstantUnpicks.put("com/indigo3d/util/RenderSystem.bindTextureToTarget(II)V",
                new ParamsConstantUnpick(glParam, null));
        staticConstantUnpicks.put("com/indigo3d/util/RenderSystem.bufferData(Ljava/nio/ByteBuffer;I)V",
                new ParamsConstantUnpick(null, glParam));
        virtualConstantUnpicks.put("com/indigo3d/util/tracker/BooleanStateTracker.<init>(I)V", glParam);
        virtualConstantUnpicks.put("com/indigo3d/util/tracker/BooleanMultiStateTracker.<init>(I)V", glParam);
        virtualConstantUnpicks.put("com/indigo3d/util/tracker/BindTextureStateTracker.<init>(I)V", glParam);
        virtualConstantUnpicks.put("com/indigo3d/util/tracker/ShadeModelStateTracker.setEnabled(I)V", glParam);
        // Minecraft specific LWJGL/OpenGL Unpicks
        virtualConstantUnpicks.put("net/minecraft/client/renderer/world/Tessellator.startDrawing(I)V", glParam);
        putVirtualConstantUnpicks.put("net/minecraft/client/renderer/block/Texture.textureTarget", glParam);
        // LWJGL/Input Unpicks
        ConstantUnpick key = GeneratedConstantUnpicks.keyboardConstantUnpick;
        staticConstantUnpicks.put("org/lwjgl/input/Keyboard.isKeyDown(I)Z", key);
        staticConstantUnpicks.put("org/lwjgl/input/Keyboard.getKeyName(I)Ljava/lang/String;", key);
        returnStaticConstantUnpicks.put("org/lwjgl/input/Keyboard.getEventKey()I", key);
        // Minecraft specific LWJGL/Input Unpicks
        virtualConstantUnpicks.put("net/minecraft/client/util/KeyBinding.<init>(Ljava/lang/String;I)V", key);
    }

    private DevelopmentSourceTransformer() {}

    static void patchForDev(DevelopmentSourceConstantData developmentSourceConstantData, ClassNode classNode) {
        final IdentityHashMap<AbstractInsnNode, InsnList> constantPatching = new IdentityHashMap<>();
        final ArrayList<AbstractInsnNode> nodesCache = new ArrayList<>();
        for (MethodNode methodNode : classNode.methods) {
            final InsnList insnList = methodNode.instructions;
            final int state = developmentSourceConstantData.methodStatus(classNode, methodNode);
            if (!constantPatching.isEmpty()) {
                constantPatching.clear();
            }
            for (AbstractInsnNode abstractInsnNode : insnList) {
                final int opcode = abstractInsnNode.getOpcode();
                if (opcode == Opcodes.INVOKESTATIC) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    ConstantUnpick constantUnpick =
                            staticConstantUnpicks.get(methodInsnNode.owner + "." +
                                    methodInsnNode.name + methodInsnNode.desc);
                    ConstantUnpick returnConstantUnpick =
                            returnStaticConstantUnpicks.get(methodInsnNode.owner + "." +
                                    methodInsnNode.name + methodInsnNode.desc);
                    if (constantUnpick != null) {
                        constantUnpick.unpick(insnList, methodInsnNode.getPrevious());
                    }
                    if (returnConstantUnpick != null) {
                        AbstractInsnNode insn = methodInsnNode.getNext().getNext();
                        if (insn != null) {
                            switch (insn.getOpcode()) {
                                case Opcodes.IF_ICMPEQ:
                                case Opcodes.IF_ICMPNE:
                                case Opcodes.IF_ICMPLE:
                                case Opcodes.IF_ICMPGE:
                                case Opcodes.IF_ICMPLT:
                                case Opcodes.IF_ICMPGT:
                                    returnConstantUnpick.unpick(insnList, methodInsnNode.getNext());
                            }
                        }
                    }
                } else if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    ConstantUnpick constantUnpick =
                            virtualConstantUnpicks.get(methodInsnNode.owner + "." +
                                    methodInsnNode.name + methodInsnNode.desc);
                    if (constantUnpick != null) {
                        constantUnpick.unpick(insnList, methodInsnNode.getPrevious());
                    }
                } else if (opcode == Opcodes.PUTSTATIC) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                    ConstantUnpick constantUnpick =
                            putStaticConstantUnpicks.get(fieldInsnNode.owner + "#" + fieldInsnNode.name);
                    if (constantUnpick != null) {
                        constantUnpick.unpick(insnList, fieldInsnNode.getPrevious());
                    }
                } else if (opcode == Opcodes.PUTFIELD) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                    ConstantUnpick constantUnpick =
                            putVirtualConstantUnpicks.get(fieldInsnNode.owner + "#" + fieldInsnNode.name);
                    if (constantUnpick != null) {
                        constantUnpick.unpick(insnList, fieldInsnNode.getPrevious());
                    }
                } else if (opcode == Opcodes.LDC) {
                    LdcInsnNode ldcInsnNode = (LdcInsnNode) abstractInsnNode;
                    if (ldcInsnNode.cst instanceof String) {
                        developmentSourceConstantData.patchStringConstant(
                                constantPatching, state, ldcInsnNode, nodesCache);
                    }
                }
            }
            for (Map.Entry<AbstractInsnNode, InsnList> patching : constantPatching.entrySet()) {
                AbstractInsnNode oldConstant = patching.getKey();
                if (oldConstant.getNext() == null) continue;
                insnList.insert(oldConstant, patching.getValue());
                insnList.remove(oldConstant);
            }
        }
    }

    public static abstract class ConstantUnpick {
        private StringBuilder testDebug;

        public abstract void unpick(InsnList insnList, AbstractInsnNode constant);

        public final ConstantUnpick setTestDebug(StringBuilder testDebug) {
            this.setTestDebugOnChilds(testDebug);
            this.testDebug = testDebug;
            return this;
        }

        void setTestDebugOnChilds(StringBuilder testDebug) {}

        protected final void testDbg(String line) {
            if (this.testDebug != null) {
                this.testDebug.append(line).append("\n");
            }
        }

        protected final void testDbgOpcode(int opcode) {
            if (this.testDebug != null) {
                this.testDebug.append("Opcode: ").append(
                        Textifier.OPCODES[opcode]).append("\n");
            }
        }

        protected final void testDbgOpcode(String text, int opcode) {
            if (this.testDebug != null) {
                this.testDebug.append(text).append(
                        Textifier.OPCODES[opcode]).append("\n");
            }
        }

        protected final void npeDbg(InsnList insnList, Object obj) {
            if (obj != null) return;
            if (this.testDebug == null) throw new NullPointerException();
            StringBuilder stringBuilder = new StringBuilder()
                    .append("\n---\n").append(this.testDebug).append("\n---\n");
            TransformerUtils.printInsnList(insnList, stringBuilder);
            throw new NullPointerException(stringBuilder.append("\n---").toString());
        }
    }

    public static final class MultiConstantUnpick extends ConstantUnpick {
        private final ConstantUnpick[] constantUnpicks;

        public MultiConstantUnpick(ConstantUnpick... constantUnpicks) {
            this.constantUnpicks = constantUnpicks;
        }

        @Override
        public void unpick(InsnList insnList, AbstractInsnNode constant) {
            for (ConstantUnpick constantUnpick : constantUnpicks) {
                if (constant.getNext() == null) break;
                constantUnpick.unpick(insnList, constant);
            }
        }

        @Override
        void setTestDebugOnChilds(StringBuilder testDebug) {
            for (ConstantUnpick constantUnpick : constantUnpicks) {
                constantUnpick.setTestDebug(testDebug);
            }
        }
    }

    public static final class ParamsConstantUnpick extends ConstantUnpick {
        private final ConstantUnpick[] constantUnpicks;

        public ParamsConstantUnpick(ConstantUnpick... constantUnpicks) {
            this.constantUnpicks = constantUnpicks;
        }

        @Override
        public void unpick(InsnList insnList, AbstractInsnNode constant) {
            boolean skipNextUnpick;
            for (int i = constantUnpicks.length - 1; i >= 0; i--) {
                skipNextUnpick = false;
                int loops = 1;
                while (loops-->0) {
                    npeDbg(insnList, constant); // <- for test
                    testDbgOpcode(constant.getOpcode());
                    switch (constant.getOpcode()) {
                        default:
                            return;
                        case Opcodes.INVOKEINTERFACE:
                        case Opcodes.INVOKEVIRTUAL:
                        case Opcodes.INVOKESPECIAL:
                            loops++;
                        case Opcodes.INVOKESTATIC: {
                            MethodInsnNode methodInsnNode = (MethodInsnNode) constant;
                            if (methodInsnNode.desc.endsWith(")V")) return;
                            loops += Type.getArgumentTypes(methodInsnNode.desc).length;
                            skipNextUnpick = true;
                            break;
                        }
                        case Opcodes.DUP:
                            loops--;
                            break;
                        case Opcodes.IADD:
                        case Opcodes.ISUB:
                        case Opcodes.IMUL:
                        case Opcodes.IDIV:
                        case Opcodes.FADD:
                        case Opcodes.FSUB:
                        case Opcodes.FMUL:
                        case Opcodes.FDIV:
                        case Opcodes.DADD:
                        case Opcodes.DSUB:
                        case Opcodes.DMUL:
                        case Opcodes.DDIV:
                        case Opcodes.IAND:
                        case Opcodes.IOR:
                        case Opcodes.IXOR:
                        case Opcodes.ISHL:
                        case Opcodes.ISHR:
                        case Opcodes.IUSHR:
                            loops++;
                        case Opcodes.ARRAYLENGTH:
                        case Opcodes.GETFIELD:
                        case Opcodes.INEG:
                        case Opcodes.FNEG:
                        case Opcodes.DNEG:
                        case Opcodes.I2D:
                        case Opcodes.I2F:
                        case Opcodes.F2I:
                        case Opcodes.D2I:
                        case Opcodes.D2F:
                        case Opcodes.L2D:
                        case Opcodes.L2I:
                        case Opcodes.L2F:
                        case Opcodes.I2L:
                        case Opcodes.D2L:
                        case Opcodes.F2L:
                            skipNextUnpick = true;
                        case Opcodes.CHECKCAST:
                        case Opcodes.I2B:
                        case Opcodes.I2C:
                        case Opcodes.I2S:
                            loops++;
                        case Opcodes.GETSTATIC:
                        case Opcodes.ACONST_NULL:
                        case Opcodes.ICONST_M1:
                        case Opcodes.ICONST_0:
                        case Opcodes.ICONST_1:
                        case Opcodes.ICONST_2:
                        case Opcodes.ICONST_3:
                        case Opcodes.ICONST_4:
                        case Opcodes.ICONST_5:
                        case Opcodes.FCONST_0:
                        case Opcodes.FCONST_1:
                        case Opcodes.FCONST_2:
                        case Opcodes.DCONST_0:
                        case Opcodes.DCONST_1:
                        case Opcodes.LCONST_0:
                        case Opcodes.LCONST_1:
                        case Opcodes.BIPUSH:
                        case Opcodes.SIPUSH:
                        case Opcodes.LDC:
                        case Opcodes.ALOAD:
                        case Opcodes.ILOAD:
                        case Opcodes.FLOAD:
                        case Opcodes.DLOAD:
                    }
                    if (loops > 0) {
                        constant = constant.getPrevious();
                    }
                    testDbg("Loops left: " + loops + " skip next: " + skipNextUnpick);
                }
                loops++;
                if (loops < 0) {
                    testDbg("Neg loop: " + loops);
                    i += loops;
                    skipNextUnpick = true;
                }
                ConstantUnpick constantUnpick = constantUnpicks[i];
                testDbg("Unpick: " + i + "/" + constantUnpicks.length + " opcode: " + Textifier.OPCODES[constant.getOpcode()]);
                if (constantUnpick != null && !skipNextUnpick) {
                    testDbg("Processing...");
                    AbstractInsnNode previous = constant.getPrevious();
                    constantUnpick.unpick(insnList, constant);
                    if (previous == null ||
                            (constant = previous.getNext()) == null) {
                        return;
                    }
                }
                constant = constant.getPrevious();
            }
        }

        @Override
        void setTestDebugOnChilds(StringBuilder testDebug) {
            for (ConstantUnpick constantUnpick : constantUnpicks) {
                if (constantUnpick != null) {
                    constantUnpick.setTestDebug(testDebug);
                }
            }
        }
    }

    public static abstract class LongConstantUnpick extends ConstantUnpick {
        @Override
        public final void unpick(InsnList insnList, AbstractInsnNode constant) {
            switch (constant.getOpcode()) {
                case Opcodes.LCONST_0:
                    unpick(insnList, constant, 0);
                    break;
                case Opcodes.LCONST_1:
                    unpick(insnList, constant, 1);
                    break;
                case Opcodes.LDC:
                    Object ldc = ((LdcInsnNode) constant).cst;
                    if (ldc instanceof Long) {
                        unpick(insnList, constant, (Long) ldc);
                    }
            }
        }

        public abstract void unpick(InsnList insnList, AbstractInsnNode constant, long value);
    }

    public static abstract class IntConstantUnpick extends ConstantUnpick {
        @Override
        public final void unpick(InsnList insnList, AbstractInsnNode constant) {
            switch (constant.getOpcode()) {
                case Opcodes.ICONST_M1:
                    unpick(insnList, constant, -1);
                    break;
                case Opcodes.ICONST_0:
                    unpick(insnList, constant, 0);
                    break;
                case Opcodes.ICONST_1:
                    unpick(insnList, constant, 1);
                    break;
                case Opcodes.ICONST_2:
                    unpick(insnList, constant, 2);
                    break;
                case Opcodes.ICONST_3:
                    unpick(insnList, constant, 3);
                    break;
                case Opcodes.ICONST_4:
                    unpick(insnList, constant, 4);
                    break;
                case Opcodes.ICONST_5:
                    unpick(insnList, constant, 5);
                    break;
                case Opcodes.BIPUSH:
                case Opcodes.SIPUSH:
                    unpick(insnList, constant, ((IntInsnNode) constant).operand);
                    break;
                case Opcodes.LDC:
                    Object ldc = ((LdcInsnNode) constant).cst;
                    if (ldc instanceof Integer) {
                        unpick(insnList, constant, (Integer) ldc);
                    }
            }
        }

        public abstract void unpick(InsnList insnList, AbstractInsnNode constant, int value);
    }

    private static abstract class StringConstantUnpick extends ConstantUnpick {
        @Override
        public void unpick(InsnList insnList, AbstractInsnNode constant) {
            if (constant.getOpcode() == Opcodes.LDC) {
                Object ldc = ((LdcInsnNode) constant).cst;
                if (ldc instanceof String) {
                    unpick(insnList, constant, (String) ldc);
                }
            }
        }

        public abstract void unpick(InsnList insnList, AbstractInsnNode constant, String value);
    }

    public static final class CheckCastConstantUnpick extends ConstantUnpick {
        private final String type;

        private CheckCastConstantUnpick(String type) {
            this.type = type;
        }

        @Override
        public void unpick(InsnList insnList, AbstractInsnNode constant) {
            if (constant.getOpcode() == Opcodes.ACONST_NULL &&
                    // For ParamsConstantUnpick compatibility we must
                    // also check if next instruction is not a check-cast
                    constant.getNext().getOpcode() != Opcodes.CHECKCAST) {
                insnList.insert(constant, new TypeInsnNode(Opcodes.CHECKCAST, this.type));
            }
        }
    }

    public static abstract class GeneratedStaticConstantUnpick extends IntConstantUnpick {
        public GeneratedStaticConstantUnpick() {}

        @Override
        public void unpick(InsnList insnList, AbstractInsnNode constant, int value) {
            FieldInsnNode fieldInsnNode = unpick(value);
            if (fieldInsnNode != null) {
                insnList.insert(constant, fieldInsnNode);
                insnList.remove(constant);
                testDbg("Found field " + fieldInsnNode.name + " for value " + value);
            } else {
                testDbg("Failed to find field for value " + value);
            }
        }

        public abstract FieldInsnNode unpick(int value);
    }

    public static abstract class IntStaticConstantUnpick extends IntConstantUnpick {
        private final String owner;

        public IntStaticConstantUnpick(String owner) {
            this.owner = owner;
        }

        @Override
        public void unpick(InsnList insnList, AbstractInsnNode constant, int value) {
            String fieldName = unpick(value);
            if (fieldName != null) {
                insnList.insert(constant,
                        new FieldInsnNode(Opcodes.GETSTATIC, this.owner, fieldName, "I"));
                insnList.remove(constant);
            }
        }

        public abstract String unpick(int value);
    }

    public static abstract class LongStaticConstantUnpick extends LongConstantUnpick {
        private final String owner;

        public LongStaticConstantUnpick(String owner) {
            this.owner = owner;
        }

        @Override
        public void unpick(InsnList insnList, AbstractInsnNode constant, long value) {
            String fieldName = unpick(value);
            if (fieldName != null) {
                insnList.insert(constant,
                        new FieldInsnNode(Opcodes.GETSTATIC, this.owner, fieldName, "J"));
                insnList.remove(constant);
            }
        }

        public abstract String unpick(long value);
    }

    public static class FlagIntStaticConstantUnpick extends IntConstantUnpick {
        private final String defaultOwner;
        private final Flag[] flags;

        public FlagIntStaticConstantUnpick(String defaultOwner, Flag... flags) {
            this.defaultOwner = defaultOwner;
            this.flags = flags;
        }

        @Override
        public void unpick(InsnList insnList, AbstractInsnNode constant, int value) {
            if (value == 0) return;
            LinkedList<Flag> flags = new LinkedList<>();
            for (Flag flag : this.flags) {
                if ((flag.value & value) == flag.value) {
                    value &= ~flag.value;
                    flags.add(flag);
                    if (value == 0)
                        break;
                }
            }
            if (flags.isEmpty()) return;
            boolean first = true;
            for (Flag flag : flags) {
                String owner = this.defaultOwner;
                if (flag.owner != null) owner = flag.owner;
                insnList.insertBefore(constant, new FieldInsnNode(Opcodes.GETSTATIC, owner, flag.name, "I"));
                if (first) {
                    first = false;
                } else {
                    insnList.insertBefore(constant, new InsnNode(Opcodes.IOR));
                }
            }
            if (value != 0) {
                insnList.insertBefore(constant, TransformerUtils.getNumberInsn(value));
                insnList.insertBefore(constant, new InsnNode(Opcodes.IOR));
            }
            insnList.remove(constant);
        }

    }

    public static class Flag {
        public final int value;
        public final String owner;
        public final String name;

        Flag(int value, String owner, String name) {
            this.value = value;
            this.owner = owner;
            this.name = name;
        }

        Flag(int value, String name) {
            this.value = value;
            this.owner = null;
            this.name = name;
        }
    }

    private static final class DebugConstantUnpick extends ConstantUnpick {
        private final StringBuilder stringBuilder = new StringBuilder();
        private final ConstantUnpick constantUnpick;

        private DebugConstantUnpick(ConstantUnpick constantUnpick) {
            this.constantUnpick = constantUnpick;
            this.constantUnpick.setTestDebug(this.stringBuilder);
        }

        @Override
        public void unpick(InsnList insnList, AbstractInsnNode constant) {
            synchronized (this.stringBuilder) {
                this.stringBuilder.setLength(0);
                this.stringBuilder.append("Unpick debug:\n");
                this.constantUnpick.unpick(insnList, constant);
                if (this.stringBuilder.length() > 0) {
                    System.out.println(this.stringBuilder);
                }
            }
        }

        @Override
        void setTestDebugOnChilds(StringBuilder testDebug) {
            throw new UnsupportedOperationException();
        }
    }
}
