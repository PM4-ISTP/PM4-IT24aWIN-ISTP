export const enum MemoryUnit {
  Byte = "B",
  MebiByte = "Mi",
  GibiByte = "Gi",
  TebiByte = "Ti"
}

export type MemorySpecification = {
    value: number,
    unit: MemoryUnit
}

export const memoryUnits: ReadonlyArray<MemoryUnit> = [MemoryUnit.Byte, MemoryUnit.MebiByte, MemoryUnit.GibiByte, MemoryUnit.TebiByte];

export const memorySpecificationToString = (memorySpecification: MemorySpecification): string => {
    return memorySpecification.value + " " + memorySpecification.unit;
}