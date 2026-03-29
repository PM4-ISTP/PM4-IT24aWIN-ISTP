"use client";

import { useState } from "react";
import { Button, FileInput, Group, NumberInput, Stack, Text } from "@mantine/core";
import { useForm } from "@mantine/form";
import { postAdminConfig } from "@/src/app/actions";

export default function AdminConfigForm() {
  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      cpuLimit: "",
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
      formData.set("kubeconfig", file);
      setValueIfNotEmpty(formData, "cpuLimit", String(values.cpuLimit));
      await postAdminConfig(formData);
      setCompleted(true);
    }
  };

  const setValueIfNotEmpty = (setInFormData: FormData, valueName: string, value: string) => {
    if (value !== "") {
      setInFormData.set(valueName, value);
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
      <NumberInput
        label="Cpu limit"
        description="How much CPU one single pod can at maximum use. Leave this field empty, if you do not want to specify a CPU limit."
        key={form.key("cpuLimit")}
        {...form.getInputProps("cpuLimit")}
        min={1}
        allowDecimal={false}
      />
      <FileInput
        withAsterisk
        label="Kubeconfig"
        description="Please upload your Kubeconfig file for the K3d cluster that manages the challenge pods."
        placeholder="Please upload your Kubeconfig here"
        key={form.key("kubeconfig")}
        disabled={form.submitting}
        required={true}
        {...form.getInputProps("kubeconfig")}
      />

      <Group justify="flex-end" mt="md">
        <Button type="submit" loading={form.submitting}>
          Submit
        </Button>
      </Group>
    </form>
  );
}
