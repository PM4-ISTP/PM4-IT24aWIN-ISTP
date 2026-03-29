"use client";

import { useState } from "react";
import { Button, FileInput, Grid, Group, NumberInput, Select, Stack, Text } from "@mantine/core";
import { useForm } from "@mantine/form";
import { postAdminConfig } from "@/src/app/actions";
import { memorySpecificationToString, MemoryUnit, memoryUnits } from "@/src/lib/memoryUnit";

export default function AdminConfigForm() {
  const kubeconfigFormKey = "kubeconfig";
  const cpuLimitFormKey = "cpuLimit";
  const memoryLimitFormKey = "memoryLimit";

  const defaultMemoryUnit = MemoryUnit.Byte;

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      cpuLimit: "",
      memoryLimit: "",
      memoryLimitUnit: defaultMemoryUnit,
      kubeconfig: null
    }
  });

  const [completed, setCompleted] = useState(false);

  const handleSubmit = async (values: typeof form.values) => {
    const formData: FormData = new FormData();
    const file: File | null = values.kubeconfig;
    if (file == null) {
      console.log("Kubeconfig file is null");
    } else {
      formData.set(kubeconfigFormKey, file);
      setCpuLimit(formData, values.cpuLimit);
      setMemoryLimit(formData, values.memoryLimit, values.memoryLimitUnit)
      await postAdminConfig(formData);
      setCompleted(true);
    }
  };

  const setCpuLimit = (setInFormData: FormData, cpuLimit: number | string) => {
    if (cpuLimit !== "") {
      setInFormData.set(cpuLimitFormKey, String(cpuLimit));
    }
  }

  const setMemoryLimit = (setInFormData: FormData, memoryLimit: number | string, memoryLimitUnit: MemoryUnit) => {
    if (typeof memoryLimit === "number") {
      setInFormData.set(memoryLimitFormKey, memorySpecificationToString({ value: memoryLimit, unit: memoryLimitUnit }))
    }
  }

  if (completed) {
    return (
      <Stack>
        <Text>Form submitted!</Text>
        <Button onClick={() => setCompleted(false)}>Reset to initial state</Button>
      </Stack>
    );
  }

  return (
    <form onSubmit={form.onSubmit(handleSubmit)} encType="multipart/form-data">
      <Text>Required fields are marked with *</Text>

      <Grid>
        <Grid.Col span={12}>
          <NumberInput
            label="CPU limit"
            description="How much CPU one single pod can at maximum use. Leave this field empty, if you do not want to specify a CPU limit."
            key={form.key(cpuLimitFormKey)}
            {...form.getInputProps(cpuLimitFormKey)}
            min={1}
            allowDecimal={false}
          />
        </Grid.Col>
        <Grid.Col span={8}>
          <NumberInput
            label="Memory limit"
            description="How much memory one single pod can at maximum use. Leave this field empty, if you do not want to specify a memory limit."
            key={form.key(memoryLimitFormKey)}
            {...form.getInputProps(memoryLimitFormKey)}
            min={1}
            allowDecimal={false}
          />
        </Grid.Col>
        <Grid.Col span={4}>
          <Select
            label="Memory limit"
            description="Please select a unit for the desired memory limit."
            key={form.key("memoryLimitUnit")}
            {...form.getInputProps("memoryLimitUnit")}
            data={memoryUnits}
            defaultValue={defaultMemoryUnit}
          />
        </Grid.Col>
        <Grid.Col span={12}>
          <FileInput
            withAsterisk
            label="Kubeconfig"
            description="Please upload your Kubeconfig file for the K3d cluster that manages the challenge pods."
            placeholder="Please upload your Kubeconfig here"
            key={form.key(kubeconfigFormKey)}
            disabled={form.submitting}
            required={true}
            {...form.getInputProps(kubeconfigFormKey)}
          />
        </Grid.Col>
      </Grid>

      <Group justify="flex-end" mt="md">
        <Button type="submit" loading={form.submitting}>
          Submit
        </Button>
      </Group>
    </form>
  );
}
