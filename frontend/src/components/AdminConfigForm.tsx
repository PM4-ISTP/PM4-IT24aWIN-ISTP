"use client";

import { useState } from "react";
import { Button, FileInput, Group, Stack, Text } from "@mantine/core";
import { useForm } from "@mantine/form";
import { postAdminConfig } from "../app/actions";

export default function AdminConfigForm() {
  const form = useForm({
    mode: "uncontrolled",
    initialValues: { kubeconfig: null },
  });

  const [completed, setCompleted] = useState(false);

  const handleSubmit = async (values: typeof form.values) => {
    const formData: FormData = new FormData();
    const file: File | null = values.kubeconfig;
    if (file == null) {
      console.log("Kubeconfig file is null");
    } else {
      formData.set("kubeconfig", file);
      await postAdminConfig(formData);
      setCompleted(true);
    }
  };

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
      <FileInput
        label="Kubeconfig"
        description="Upload Kubeconfig here"
        placeholder="Please upload your Kubeconfig"
        key={form.key("kubeconfig")}
        disabled={form.submitting}
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
