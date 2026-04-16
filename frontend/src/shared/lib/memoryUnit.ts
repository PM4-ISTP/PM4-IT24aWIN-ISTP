export const enum MemoryUnit {
  Byte = "B",
  MebiByte = "Mi",
  GibiByte = "Gi",
  TebiByte = "Ti",
}

export type MemorySpecification = {
  value: number;
  unit: MemoryUnit;
};

export const memoryUnits: ReadonlyArray<MemoryUnit> = [
  MemoryUnit.Byte,
  MemoryUnit.MebiByte,
  MemoryUnit.GibiByte,
  MemoryUnit.TebiByte,
];

export const memorySpecificationToString = (memorySpecification: MemorySpecification): string => {
  return memorySpecification.value + memorySpecification.unit;
};

export const stringToMemorySpecification = (specificationAsString: string): MemorySpecification => {
  const match = specificationAsString.match(/^(\d+)(B|Mi|Gi|Ti)$/);
  if (!match) {
    throw new Error(specificationAsString + " is not a valid memory specification.");
  }
  return {
    value: Number.parseInt(match[1]),
    unit: match[2] as MemoryUnit,
  };
};
