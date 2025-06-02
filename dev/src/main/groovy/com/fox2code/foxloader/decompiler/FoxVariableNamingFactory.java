package com.fox2code.foxloader.decompiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IVariableNameProvider;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructLocalVariableTableAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructMethodParametersAttribute;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.Pair;
import org.objectweb.asm.Type;

import java.util.*;

final class FoxVariableNamingFactory implements IVariableNamingFactory, CodeConstants {
    private static final boolean DEBUG = false;
    static final FoxVariableNamingFactory INSTANCE = new FoxVariableNamingFactory();

    private FoxVariableNamingFactory() {}

    @Override
    public @NotNull IVariableNameProvider createFactory(final StructMethod structMethod) {
        return new FoxVarNameProvider(structMethod);
    }

    public static boolean isVarOrParam(String name) {
        boolean isVar = name.length() >= 4 && name.startsWith("var") && Character.isDigit(name.charAt(3));
        boolean isParam = name.length() >= 6 && name.startsWith("param") && Character.isDigit(name.charAt(5));
        return isVar || isParam;
    }

    private static class FoxVarNameProvider implements IVariableNameProvider {
        final StructMethod structMethod;

        StructClass structClass;
        ArrayList<NamingStructMethod> directExtenders;
        NamingStructMethod namingStructMethod;
        FoxVarNameProvider parent;

        private FoxVarNameProvider(StructMethod structMethod) {
            this.structMethod = structMethod;
            this.directExtenders = null;
        }

        @Override
        public Map<VarVersionPair, String> rename(Map<VarVersionPair, Pair<VarType, String>> map) {
            if (map == null || map.isEmpty()) return Collections.emptyMap();
            HashMap<VarVersionPair, String> renamingMap = null;
            final int maxParamCount = this.getNamingStructMethod().getMaxParamCount();
            for (Map.Entry<VarVersionPair, Pair<VarType, String>> entry : map.entrySet()) {
                if (entry.getKey().var >= maxParamCount) continue;
                Pair<VarType, String> pair = entry.getValue();
                String newName = this.renameParameterEx(0, pair.a, pair.b, entry.getKey().var, false);
                if (newName != null && !newName.equals(pair.b)) {
                    if (renamingMap == null) {
                        renamingMap = new HashMap<>();
                    }
                    renamingMap.put(entry.getKey(), newName);
                }
            }
            return renamingMap != null ? renamingMap : Collections.emptyMap();
        }

        @Override
        public void addParentContext(IVariableNameProvider variableNameProvider) {
            if (variableNameProvider instanceof FoxVarNameProvider) {
                this.parent = (FoxVarNameProvider) variableNameProvider;
            }
        }

        @Override
        public String renameAbstractParameter(String name, int index) {
            return this.renameParameterEx(ACC_ABSTRACT, null, name, index, true);
        }

        @Override
        public String renameParameter(int flags, VarType type, String name, int index) {
            return this.renameParameterEx(flags, type, name, index, false);
        }

        public String renameParameterEx(int flags, VarType type, String name, int index, boolean abs) {
            if (name != null && !name.trim().isEmpty() && !isVarOrParam(name)) {
                return name; // Name is already defined properly
            }
            if ((this.structMethod.getAccessFlags() & (ACC_PRIVATE | ACC_FINAL)) != 0) {
                if (DEBUG) {
                    System.out.println("DEBUG: Skipping private or final " +
                            this.structMethod.getClassQualifiedName() +
                            " " + flags + " " + type + " \"" + name + "\" " + index + " " + abs);
                }
                return name; // Don't check extenders if we can't have any.
            }
            HashMap<String, int[]> counters = new HashMap<>();
            int maxCounter = 0;
            String newVarName = name;
            for (NamingStructMethod extender : this.getDirectExtenders()) {
                if (DEBUG) {
                    System.out.println("DEBUG: Checking " + extender +
                            " for " + this.structMethod.getClassQualifiedName() +
                            "." + this.structMethod.getName() + "()");
                }
                String paramName = extender.getParamName(index);
                if (paramName == null) {
                    if (DEBUG) {
                        System.out.println("No param name at " + index + " in " + extender.getClassQualifiedName());
                    }
                    continue;
                }
                int[] counter = counters.get(paramName);
                if (counter == null) {
                    if (isVarOrParam(paramName) ||
                            getNamingStructMethod().isVarNameAlreadyUsed(paramName)) {
                        if (DEBUG) {
                            System.out.println("Invalid new name " + paramName + " to replace " + name);
                        }
                        continue;
                    }
                    counter = new int[]{0};
                    counters.put(paramName, counter);
                }
                counter[0]++;
                if (maxCounter < counter[0]) {
                    newVarName = paramName;
                    maxCounter = counter[0];
                }
            }
            if (DEBUG) {
                System.out.println("DEBUG: " + this.structMethod.getClassQualifiedName() +
                        " " + flags + " " + type + " \"" + name + "\" " + index + " " + abs + " -> " + newVarName);
            }
            return newVarName;
        }

        StructClass getStructClass() {
            if (this.structClass == null) {
                this.structClass = DecompilerContext.getStructContext()
                        .getClass(structMethod.getClassQualifiedName());
            }
            return this.structClass;
        }

        public NamingStructMethod getNamingStructMethod() {
            if (this.namingStructMethod == null) {
                this.namingStructMethod = new NamingStructMethod(this.structMethod);
            }
            return this.namingStructMethod;
        }

        private ArrayList<NamingStructMethod> getDirectExtenders() {
            if (this.directExtenders == null) {
                this.directExtenders = new ArrayList<>();
                String qualifiedName = this.structMethod.getClassQualifiedName();
                StructClass structClass = this.getStructClass();
                if (structClass == null || (structClass.getAccessFlags() & ACC_FINAL) != 0) {
                    return this.directExtenders;
                }
                ArrayList<StructClass> candidates = new ArrayList<>();
                if ((structClass.getAccessFlags() & ACC_INTERFACE) != 0) {
                    for (StructClass anyStructClass : DecompilerContext.getStructContext().getOwnClasses()) {
                        for (String interfaceName : anyStructClass.getInterfaceNames()) {
                            if (qualifiedName.equals(interfaceName)) {
                                candidates.add(anyStructClass);
                                break;
                            }
                        }
                    }
                } else {
                    for (StructClass anyStructClass : DecompilerContext.getStructContext().getOwnClasses()) {
                        if (anyStructClass.superClass != null) {
                            if (qualifiedName.equals(anyStructClass.superClass.getString())) {
                                candidates.add(anyStructClass);
                                continue;
                            }
                            for (StructClass anySuperClasses : anyStructClass.getAllSuperClasses()) {
                                if (anySuperClasses == this.structClass) {
                                    candidates.add(anyStructClass);
                                    break;
                                }
                            }
                        }
                    }
                }
                String methodName = this.structMethod.getName();
                String methodDesc = this.structMethod.getDescriptor();

                for (StructClass candidate : candidates) {
                    StructMethod candidateMethod = candidate.getMethod(methodName, methodDesc);
                    if (candidateMethod != null && (candidateMethod.getAccessFlags() & ACC_ABSTRACT) == 0) {
                        this.directExtenders.add(new NamingStructMethod(candidateMethod));
                    }
                }
            }
            return this.directExtenders;
        }
    }

    private static class NamingStructMethod {
        private final String classQualifiedName;
        private final ArrayList<String> paramNames = new ArrayList<>();
        private final HashSet<String> reservedNames = new HashSet<>();

        public NamingStructMethod(StructMethod structMethod) {
            classQualifiedName = structMethod.getClassQualifiedName();
            StructMethodParametersAttribute structMethodParametersAttribute =
                    structMethod.getAttribute(StructGeneralAttribute.ATTRIBUTE_METHOD_PARAMETERS);
            StructLocalVariableTableAttribute structLocalVariableTableAttribute =
                    structMethod.getAttribute(StructGeneralAttribute.ATTRIBUTE_LOCAL_VARIABLE_TABLE);
            Type[] arguments = Type.getArgumentTypes(structMethod.getDescriptor());
            final int argIndexInitial = (structMethod.getAccessFlags() & ACC_STATIC) == 0 ? 1 : 0;
            int argIndexMax = argIndexInitial;
            for (Type argument : arguments) {
                argIndexMax += argument.getSize();
            }
            for (int i = 0; i < argIndexMax; i++) {
                paramNames.add(null);
            }
            if (argIndexInitial == 1) {
                paramNames.set(0, "this");
            }
            if (structMethodParametersAttribute != null) {
                int typeIndex = 0, argIndex = 0;
                for (StructMethodParametersAttribute.Entry entry : structMethodParametersAttribute.getEntries()) {
                    typeIndex++;
                    argIndex += arguments[typeIndex].getSize();
                    paramNames.set(argIndex, entry.myName);
                    if (!isVarOrParam(entry.myName)) {
                        reservedNames.add(entry.myName);
                    }
                }
            }
            if (structLocalVariableTableAttribute != null) {
                int argIndex = argIndexInitial;
                for (Type type : arguments) {
                    StructLocalVariableTableAttribute.LocalVariable localVariable =
                            structLocalVariableTableAttribute.matchingVars(argIndex).findFirst().orElse(null);
                    if (localVariable != null) {
                        if (!isVarOrParam(localVariable.getName())) {
                            reservedNames.add(localVariable.getName());
                        }
                        if (argIndex < argIndexMax &&
                                paramNames.get(argIndex) == null) {
                            paramNames.set(argIndex, localVariable.getName());
                        }
                    }
                    argIndex += type.getSize();
                }
            }
        }

        public String getParamName(int index) {
            return index >= this.paramNames.size() ? null : this.paramNames.get(index);
        }

        public boolean isVarNameAlreadyUsed(String name) {
            return this.reservedNames.contains(name);
        }

        public String getClassQualifiedName() {
            return this.classQualifiedName;
        }

        public int getMaxParamCount() {
            return this.reservedNames.size();
        }

        @Override
        public String toString() {
            return "NamingStructMethod{" +
                    "classQualifiedName='" + classQualifiedName + '\'' +
                    ", paramNames=" + paramNames +
                    ", reservedNames=" + reservedNames +
                    '}';
        }
    }
}
