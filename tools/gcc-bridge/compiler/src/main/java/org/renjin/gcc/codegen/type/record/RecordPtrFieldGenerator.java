package org.renjin.gcc.codegen.type.record;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.renjin.gcc.codegen.expr.AbstractExprGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.type.FieldGenerator;
import org.renjin.gcc.codegen.type.record.unit.DereferencedUnitRecordPtr;
import org.renjin.gcc.codegen.type.record.unit.RecordUnitPtrGenerator;
import org.renjin.gcc.gimple.type.GimplePointerType;
import org.renjin.gcc.gimple.type.GimpleType;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

public class RecordPtrFieldGenerator extends FieldGenerator {
  private String className;
  private String fieldName;
  private RecordTypeStrategy strategy;

  public RecordPtrFieldGenerator(String className, String fieldName, RecordTypeStrategy strategy) {
    this.className = className;
    this.fieldName = fieldName;
    this.strategy = strategy;
  }

  @Override
  public GimpleType getType() {
    return new GimplePointerType(strategy.getRecordType());
  }

  @Override
  public void emitInstanceField(ClassVisitor cv) {
    emitField(ACC_PUBLIC, cv);
  }

  private void emitField(int access, ClassVisitor cv) {
    cv.visitField(access, fieldName, strategy.getJvmType().getDescriptor(), null, null).visitEnd();
  }

  @Override
  public ExprGenerator memberExprGenerator(ExprGenerator instanceGenerator) {
    return new Member(instanceGenerator);
  }

  private class Member extends AbstractExprGenerator implements RecordUnitPtrGenerator {

    private ExprGenerator instanceGenerator;

    public Member(ExprGenerator instanceGenerator) {
      this.instanceGenerator = instanceGenerator;
    }

    @Override
    public GimpleType getGimpleType() {
      return new GimplePointerType(strategy.getRecordType());
    }

    @Override
    public void emitPushRecordRef(MethodVisitor mv) {
      instanceGenerator.emitPushRecordRef(mv);
      mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, strategy.getJvmType().getDescriptor());
    }

    @Override
    public void emitStore(MethodVisitor mv, ExprGenerator valueGenerator) {
      instanceGenerator.emitPushRecordRef(mv);
      valueGenerator.emitPushRecordRef(mv);
      mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, strategy.getJvmType().getDescriptor());
    }

    @Override
    public ExprGenerator valueOf() {
      return new DereferencedUnitRecordPtr(strategy, this);
    }
  }
}
