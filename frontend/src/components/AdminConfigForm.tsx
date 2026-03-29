"use client";

import { useEffect, useState } from "react";
import { Button, Fieldset, FileInput, Grid, Group, NumberInput, Select, Stack, Text } from "@mantine/core";
import { useForm } from "@mantine/form";
import { deleteAdminConfig, getAdminConfig, postAdminConfig, putAdminConfig } from "@/src/app/actions";
import { MemorySpecification, memorySpecificationToString, MemoryUnit, memoryUnits, stringToMemorySpecification } from "@/src/lib/memoryUnit";

export default function AdminConfigForm() {
  type AdminConfigResponse = {
    kubeconfigUploaded: boolean,
    cpuLimit: string | null,
    memoryLimit: string | null,
    updatedAt: string | null
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
        if (!adminConfigResponse.kubeconfigUploaded && value === null) {
          return "You need to upload a Kubeconfig file.";
        } else {
          return null;
        }
      },
      cpuLimit: (value) => {
        if (adminConfigResponse.kubeconfigUploaded && value === null) {
          return "You need to define a CPU limit or create a new configuration by deleting the current configuration.";
        } else {
          return null;
        }
      },
      memoryLimit: (value) => {
        if (adminConfigResponse.kubeconfigUploaded && value === null) {
          return "You need to define a memory limit or create a new configuration by deleting the current configuration.";
        } else {
          return null;
        }
      }
    }
  });

  // const [isCreateMode, setIsCreateMode] = useState(true);
  const [adminConfigResponse, setAdminConfigResponse] = useState<AdminConfigResponse>({
    kubeconfigUploaded: false,
    cpuLimit: null,
    memoryLimit: null,
    updatedAt: null
  });
  const [completed, setCompleted] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadConfiguration();
  }, []);

  useEffect(() => {
    initializeForm();
  }, [adminConfigResponse])

  const loadConfiguration = async () => {
    document.getElementById(formId)?.setAttribute("disabled", "");
    const response: AdminConfigResponse = await getAdminConfig();
    setAdminConfigResponse(response);
  }

  const initializeForm = () => {
    try {
      const memorySpecification = adminConfigResponse.memoryLimit === null ? null : stringToMemorySpecification(adminConfigResponse.memoryLimit);
      const hasMemorySpecification = memorySpecification !== null;
      form.setValues({
        cpuLimit: adminConfigResponse.cpuLimit === null ? "" : adminConfigResponse.cpuLimit,
        memoryLimit: hasMemorySpecification ? String(memorySpecification!.value) : "",
        memoryLimitUnit: hasMemorySpecification ? memorySpecification!.unit : defaultMemoryUnit,
        kubeconfig: null
      });
      if (adminConfigResponse.kubeconfigUploaded) {
        // setIsCreateMode(false);
      }
    } catch (e) {
      console.log(e);
      setErrorMessage("It was not possible to load the K3d configuration. Please check the server log. It is possible, that the configuration is corrupted. In this case, please create a new configuration.");
    }
    document.getElementById(formId)?.removeAttribute("disabled");
  }

  const reloadForm = () => {
    loadConfiguration();
    setCompleted(false);
  }

  const handleSubmit = async (values: typeof form.values) => {
    const formData: FormData = new FormData();
    const file: File | null = values.kubeconfig;
    if (file === null) {
      console.log("Kubeconfig file is null");
    } else {
      formData.set(kubeconfigFormKey, file);
      setCpuLimit(formData, values.cpuLimit);
      setMemoryLimit(formData, values.memoryLimit, values.memoryLimitUnit);
      !adminConfigResponse.kubeconfigUploaded ? await postAdminConfig(formData) : await putAdminConfig(formData);
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
        <Button onClick={() => reloadForm()}>Reset to initial state</Button>
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
              required={adminConfigResponse.cpuLimit !== null}
              withAsterisk={adminConfigResponse.cpuLimit !== null}
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
              required={adminConfigResponse.memoryLimit !== null}
              withAsterisk={adminConfigResponse.memoryLimit !== null}
            />
          </Grid.Col>
          <Grid.Col span={4}>
            <Select
              label="Memory unit"
              description="Please select a unit for the desired memory limit."
              key={form.key("memoryLimitUnit")}
              {...form.getInputProps("memoryLimitUnit")}
              data={memoryUnits}
              defaultValue={defaultMemoryUnit}
              required={adminConfigResponse.memoryLimit !== null}
              withAsterisk={adminConfigResponse.memoryLimit !== null}
            />
          </Grid.Col>
          <Grid.Col span={12}>
            <FileInput
              withAsterisk={!adminConfigResponse.kubeconfigUploaded}
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
            {!adminConfigResponse.kubeconfigUploaded ? "Create K3d configuration" : "Update K3d configuration"}
          </Button>
          <Button type="button" onClick={() => handleDelete()} loading={form.submitting}>
            Delete K3d configuration
          </Button>
        </Group>
      </Fieldset>
    </form>
  );
}
