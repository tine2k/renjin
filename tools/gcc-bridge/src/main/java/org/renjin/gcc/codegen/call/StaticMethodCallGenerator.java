package org.renjin.gcc.codegen.call;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.renjin.gcc.InternalCompilerException;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.type.ParamStrategy;
import org.renjin.gcc.codegen.type.ReturnStrategy;
import org.renjin.gcc.codegen.type.TypeOracle;
import org.renjin.gcc.codegen.type.primitive.PrimitiveConstGenerator;
import org.renjin.gcc.gimple.type.GimpleIndirectType;
import org.renjin.gcc.gimple.type.GimplePrimitiveType;
import org.renjin.gcc.gimple.type.GimpleType;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Generates a call to an existing JVM method.
 */
public class StaticMethodCallGenerator implements CallGenerator {
  
  private TypeOracle typeOracle;
  private Method method;

  private List<ParamStrategy> paramStrategies = null;
  private ReturnStrategy returnStrategy = null;

  public StaticMethodCallGenerator(TypeOracle typeOracle, Method method) {
    this.typeOracle = typeOracle;
    this.method = method;
  }

  private ReturnStrategy returnGenerator() {
    if(returnStrategy == null) {
      returnStrategy = typeOracle.forReturnValue(method);
    }
    return returnStrategy;
  }

  @Override
  public void emitCall(MethodVisitor mv, List<ExprGenerator> argumentGenerators) {

    checkArity(argumentGenerators);

    // The number of fixed (gimple) parameters expected, excluding var args
    // the number of Jvm arguments may be greater
    int fixedArgCount = paramStrategies.size();


    // Push all (fixed) parameters on the stack
    for (int i = 0; i < fixedArgCount; i++) {
      ParamStrategy paramStrategy = getParamStrategies().get(i);
      paramStrategy.emitPushParameter(mv, argumentGenerators.get(i));
    }
    
    // if this method accepts var args, then we pass the remaining arguments as an Object[] array
    if(method.isVarArgs()) {
      int varArgCount = argumentGenerators.size() - fixedArgCount;
      PrimitiveConstGenerator.emitInt(mv, varArgCount);
      mv.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class));
      
      for(int i=0;i<varArgCount;++i) {
        mv.visitInsn(Opcodes.DUP);
        PrimitiveConstGenerator.emitInt(mv, i);
        pushVarArg(mv, argumentGenerators.get(fixedArgCount + i));
        mv.visitInsn(Opcodes.AASTORE);
      }
    }
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(method.getDeclaringClass()),
        method.getName(), Type.getMethodDescriptor(method), false);
  }

  @Override
  public void emitCallAndPopResult(MethodVisitor mv, List<ExprGenerator> argumentGenerators) {
    emitCall(mv, argumentGenerators);
    switch (Type.getReturnType(method).getSize()) {
      case 0:
        // NOOP
        break;
      case 1:
        mv.visitInsn(Opcodes.POP);
        break;
      case 2:
        mv.visitInsn(Opcodes.POP2);
        break;
    }
  }

  private void pushVarArg(MethodVisitor mv, ExprGenerator exprGenerator) {
    GimpleType type = exprGenerator.getGimpleType();
    if(type instanceof GimplePrimitiveType) {
      exprGenerator.emitPushBoxedPrimitiveValue(mv);
    } else if(type instanceof GimpleIndirectType) {
      exprGenerator.emitPushPointerWrapper(mv);
    } else {
      throw new UnsupportedOperationException("type: " + type);
    }
  }

  private void checkArity(List<ExprGenerator> argumentGenerators) {
    if(method.isVarArgs()) {
      if(argumentGenerators.size() < getParamStrategies().size()) {
        throw new InternalCompilerException(String.format(
            "Arity mismatch: expected at least %d args to method %s.%s(), called with %d" ,
            paramStrategies.size(),
            method.getDeclaringClass().getName(),
            method.getName(),
            argumentGenerators.size()));
      }  
    } else {
      if(argumentGenerators.size() != getParamStrategies().size()) {
        throw new InternalCompilerException(String.format(
            "Arity mismatch: expected %d args to method %s.%s(), called with %d" ,
            paramStrategies.size(),
            method.getDeclaringClass().getName(),
            method.getName(),
            argumentGenerators.size()));
      }
    }
    
  }

  private List<ParamStrategy> getParamStrategies() {
    if(paramStrategies == null) {
      paramStrategies = typeOracle.forParameterTypesOf(method);
    }
    return paramStrategies;
  }

  @Override
  public ExprGenerator expressionGenerator(GimpleType returnType, List<ExprGenerator> argumentGenerators) {
    return returnGenerator().callExpression(this, argumentGenerators);
  }
}
