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
  return memorySpecification.value + " " + memorySpecification.unit;
};

export const stringToMemorySpecification = (specificationAsString: string): MemorySpecification => {
  const parts: string[] = specificationAsString.split(" ");
  const valueAsString = parts[0];
  const unitAsString = parts[1];

  if (!Object.values(memoryUnits).includes(unitAsString as MemoryUnit)) {
    throw new Error(unitAsString + " is not a valid memory unit.");
  }

  return {
    value: Number.parseInt(valueAsString),
    unit: unitAsString as MemoryUnit,
  };
};
