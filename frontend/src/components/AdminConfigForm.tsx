"use client";

import { useEffect, useState } from "react";
import { Button, Fieldset, FileInput, Grid, Group, NumberInput, Select, Stack, Text } from "@mantine/core";
import { useForm } from "@mantine/form";
import { deleteAdminConfig, getAdminConfig, postAdminConfig, putAdminConfig } from "@/src/app/actions";
import { MemorySpecification, memorySpecificationToString, MemoryUnit, memoryUnits, stringToMemorySpecification } from "@/src/lib/memoryUnit";

export default function AdminConfigForm() {
  type AdminConfigResponse = {
    kubeconfigUploaded: boolean,
    cpuLimit: string,
    memoryLimit: string,
    updatedAt: string
  }

  const formId = "admin-config-form";
  const infoFieldId = "admin-config-form-info-field";

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
    },
    validate: {
      kubeconfig: (value) => {
        if (isCreateMode && value == null) {
          return "You need to upload a Kubeconfig file.";
        } else {
          return null;
        }
      }
    }
  });

  const [isCreateMode, setIsCreateMode] = useState(true);
  const [completed, setCompleted] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadConfiguration();
  }, [completed]);

  const loadConfiguration = async () => {
    document.getElementById(formId)?.setAttribute("disabled", "");
    try {
      const response: AdminConfigResponse = await getAdminConfig();
      let memorySpecification = response.memoryLimit == null ? null : stringToMemorySpecification(response.memoryLimit);
      initializeForm(response, memorySpecification);
    } catch (e) {
      setErrorMessage("It was not possible to load the K3d configuration. Please check the server log. It is possible, that the configuration is corrupted. In this case, please create a new configuration.");
    }
    document.getElementById(formId)?.removeAttribute("disabled");
  }

  const initializeForm = (response: AdminConfigResponse, memorySpecification: MemorySpecification | null) => {
      form.initialize({
        cpuLimit: response.cpuLimit == null ? "" : response.cpuLimit,
        memoryLimit: memorySpecification == null ? "" : String(memorySpecification.value),
        memoryLimitUnit: memorySpecification == null ? defaultMemoryUnit : memorySpecification.unit,
        kubeconfig: null
      });
      if (response.kubeconfigUploaded) {
        setIsCreateMode(false);
      }
  }

  const handleSubmit = async (values: typeof form.values) => {
    const formData: FormData = new FormData();
    const file: File | null = values.kubeconfig;
    if (file == null) {
      console.log("Kubeconfig file is null");
    } else {
      formData.set(kubeconfigFormKey, file);
      setCpuLimit(formData, values.cpuLimit);
      setMemoryLimit(formData, values.memoryLimit, values.memoryLimitUnit);
      isCreateMode ? await postAdminConfig(formData) : await putAdminConfig(formData);
      setCompleted(true);
    }
  }

  const handleDelete = async () => {
      await deleteAdminConfig();
      setCompleted(true);
  }

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
      <Text fw={600} size="lg">
        K3d Configuration
      </Text>
      <Text id={infoFieldId} c="red" size="sm" mt={4}>{errorMessage}</Text>
      <Text c="dimmed" size="sm" mt={4}>Required fields are marked with *</Text>

      <Fieldset id={formId} disabled>
        <Grid>
          <Grid.Col span={12}>
            <NumberInput
              label="CPU limit"
              description="How much CPU one single pod can at maximum use. Leave this field empty, if you do not want to specify a CPU limit."
              key={form.key(cpuLimitFormKey)}
              {...form.getInputProps(cpuLimitFormKey)}
              min={1}
              allowNegative={false}
              allowDecimal={false}
              clampBehavior="strict"
            />
          </Grid.Col>
          <Grid.Col span={8}>
            <NumberInput
              label="Memory limit"
              description="How much memory one single pod can at maximum use. Leave this field empty, if you do not want to specify a memory limit."
              key={form.key(memoryLimitFormKey)}
              {...form.getInputProps(memoryLimitFormKey)}
              min={1}
              allowNegative={false}
              allowDecimal={false}
              clampBehavior="strict"
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
              withAsterisk={isCreateMode}
              label="Kubeconfig"
              description="Please upload your Kubeconfig file for the K3d cluster that manages the challenge pods."
              placeholder="Please upload your Kubeconfig here"
              key={form.key(kubeconfigFormKey)}
              disabled={form.submitting}
              {...form.getInputProps(kubeconfigFormKey)}
            />
          </Grid.Col>
        </Grid>

        <Group justify="flex-end" mt="md">
          <Button type="submit" loading={form.submitting}>
            {isCreateMode ? "Create K3d configuration" : "Update K3d configuration"}
          </Button>
          <Button type="button" onClick={() => handleDelete()} loading={form.submitting}>
            Delete K3d configuration
          </Button>
        </Group>
      </Fieldset>
    </form>
  );
}
