package org.renjin.gcc.codegen.type.record;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.objectweb.asm.Type;
import org.renjin.gcc.InternalCompilerException;
import org.renjin.gcc.codegen.RecordClassGenerator;
import org.renjin.gcc.codegen.call.MallocGenerator;
import org.renjin.gcc.codegen.expr.ExprFactory;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.type.*;
import org.renjin.gcc.codegen.var.VarAllocator;
import org.renjin.gcc.gimple.GimpleVarDecl;
import org.renjin.gcc.gimple.expr.GimpleConstructor;
import org.renjin.gcc.gimple.type.GimpleArrayType;
import org.renjin.gcc.gimple.type.GimplePointerType;
import org.renjin.gcc.runtime.ObjectPtr;

import java.util.List;
import java.util.Map;

/**
 * Creates generators for variables and values of type {@code GimpleRecordType}
 */
public class RecordTypeStrategy extends TypeStrategy {
  private final RecordClassGenerator generator;

  public RecordTypeStrategy(RecordClassGenerator generator) {
    this.generator = generator;
  }

  @Override
  public TypeStrategy pointerTo() {
    return new Pointer();
  }

  @Override
  public VarGenerator varGenerator(GimpleVarDecl decl, VarAllocator allocator) {
    return new RecordVarGenerator(generator, allocator.reserve(decl.getName(), generator.getType()));
  }

  @Override
  public TypeStrategy arrayOf(GimpleArrayType arrayType) {
    return new Array(arrayType);
  }

  @Override
  public FieldGenerator fieldGenerator(String className, String fieldName) {
    return new RecordFieldGenerator(className, fieldName, generator);
  }

  @Override
  public FieldGenerator addressableFieldGenerator(String className, String fieldName) {
    return new AddressableRecordField(className, fieldName, generator);
  }

  @Override
  public ExprGenerator constructorExpr(ExprFactory exprFactory, GimpleConstructor value) {
    Map<String, ExprGenerator> fields = Maps.newHashMap();
    for (GimpleConstructor.Element element : value.getElements()) {
      ExprGenerator fieldValue = exprFactory.findGenerator(element.getValue());
      fields.put(element.getFieldName(), fieldValue);
    }
    return new RecordConstructor(generator, fields);
  }

  public class Array extends TypeStrategy {

    private GimpleArrayType arrayType;

    public Array(GimpleArrayType arrayType) {
      this.arrayType = arrayType;
    }

    @Override
    public FieldGenerator fieldGenerator(String className, String fieldName) {
      return new RecordArrayFieldGenerator(className, fieldName, generator, arrayType);
    }

    @Override
    public FieldGenerator addressableFieldGenerator(String className, String fieldName) {
      return fieldGenerator(className, fieldName);
    }

    @Override
    public VarGenerator varGenerator(GimpleVarDecl decl, VarAllocator allocator) {
      return new RecordArrayVarGenerator(arrayType, generator, 
          allocator.reserveArrayRef(decl.getName(), generator.getType()));
    }
    
    @Override
    public ExprGenerator constructorExpr(ExprFactory exprFactory, GimpleConstructor value) {

      if(arrayType.getElementCount() != value.getElements().size()) {
        throw new InternalCompilerException(String.format(
            "array type defined as size of %d, only %d constructors provided",
              arrayType.getElementCount(), value.getElements().size()));
      }

      List<ExprGenerator> elements = Lists.newArrayList();
      for (GimpleConstructor.Element element : value.getElements()) {
        GimpleConstructor elementValue = (GimpleConstructor) element.getValue();
        ExprGenerator elementConstructor = exprFactory.findGenerator(elementValue);
        elements.add(elementConstructor);
      }

      return new RecordArrayConstructor(generator, arrayType, elements);
    }
  }

  public class Pointer extends TypeStrategy {

    @Override
    public ParamStrategy getParamStrategy() {
      return new RecordUnitPtrParamStrategy(generator);
    }

    @Override
    public FieldGenerator fieldGenerator(String className, String fieldName) {
      return new RecordPtrFieldGenerator(className, fieldName, generator);
    }

    @Override
    public ReturnStrategy getReturnStrategy() {
      return new RecordUnitPtrReturnStrategy(generator);
    }


    @Override
    public VarGenerator varGenerator(GimpleVarDecl decl, VarAllocator allocator) {
      if(decl.isAddressable()) {
        return new AddressableRecordPtrVarGenerator(generator, 
            allocator.reserveArrayRef(decl.getName(), generator.getType()));
        
      } else {
        return new RecordPtrVarGenerator(generator, 
            allocator.reserve(decl.getName(), generator.getType()));
      }
    }

    @Override
    public TypeStrategy pointerTo() {
      return new PointerPointer();
    }

    @Override
    public ExprGenerator mallocExpression(ExprGenerator size) {
      return new RecordMallocGenerator(generator, size);
    }
  }
  
  public class PointerPointer extends TypeStrategy {

    @Override
    public ParamStrategy getParamStrategy() {
      return new RecordUnitPtrPtrParamStrategy(generator);
    }

    @Override
    public VarGenerator varGenerator(GimpleVarDecl decl, VarAllocator allocator) {
      return new RecordPtrPtrVarGenerator(generator, 
          allocator.reserve(decl.getName(), ObjectPtr.class), 
          allocator.reserveInt(decl.getName() + "$offset"));
    }

    @Override
    public ExprGenerator mallocExpression(ExprGenerator size) {
      return new MallocGenerator(
          generator.getGimpleType().pointerTo().pointerTo(),
          Type.getType(ObjectPtr.class), 
          GimplePointerType.SIZE_OF, 
          size);
    }

    @Override
    public ReturnStrategy getReturnStrategy() {
      return new RecordPtrPtrReturnStrategy();
    }
  }
}