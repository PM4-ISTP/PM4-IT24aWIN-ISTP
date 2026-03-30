"use client";

import { useEffect, useState } from "react";
import {
  Button,
  Fieldset,
  FileInput,
  Grid,
  Group,
  NumberInput,
  Select,
  Stack,
  Text,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import {
  deleteAdminConfig,
  getAdminConfig,
  postAdminConfig,
  putAdminConfig,
} from "@/src/app/actions";
import {
  memorySpecificationToString,
  MemoryUnit,
  memoryUnits,
  stringToMemorySpecification,
} from "@/src/lib/memoryUnit";

export default function AdminConfigForm() {
  type AdminConfigResponse = {
    kubeconfigUploaded: boolean;
    cpuLimit: string | null;
    memoryLimit: string | null;
    updatedAt: string | null;
  };

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
      kubeconfig: null,
    },
    validate: {
      // In Firefox, the required attribute on FileInput does nothing. Therefor, custom validation is needed.
      kubeconfig: (value) => {
        if (!adminConfigResponse.kubeconfigUploaded && value === null) {
          return "You need to upload a Kubeconfig file.";
        } else {
          return null;
        }
      },
    },
  });

  const [adminConfigResponse, setAdminConfigResponse] = useState<AdminConfigResponse>({
    kubeconfigUploaded: false,
    cpuLimit: null,
    memoryLimit: null,
    updatedAt: null,
  });
  const [completed, setCompleted] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  async function loadConfiguration() {
    document.getElementById(formId)?.setAttribute("disabled", "");
    try {
      const response: AdminConfigResponse = await getAdminConfig();
      setAdminConfigResponse(response);
    } catch (e) {
      setErrorMessage(
        "It was not possible to load the admin configuration: " + (e as Error).message
      );
    }
  }

  /* eslint-disable react-hooks/exhaustive-deps -- The dependencies array must be empty, because this effect should only get executed on mount. */
  useEffect(() => {
    void loadConfiguration();
  }, []);
  /* eslint-enable react-hooks/exhaustive-deps */

  const initializeForm = () => {
    try {
      const memorySpecification =
        adminConfigResponse.memoryLimit === null
          ? null
          : stringToMemorySpecification(adminConfigResponse.memoryLimit);
      const hasMemorySpecification = memorySpecification !== null;
      form.setValues({
        cpuLimit: adminConfigResponse.cpuLimit === null ? "" : adminConfigResponse.cpuLimit,
        memoryLimit: hasMemorySpecification ? String(memorySpecification.value) : "",
        memoryLimitUnit: hasMemorySpecification ? memorySpecification.unit : defaultMemoryUnit,
        kubeconfig: null,
      });
    } catch (e) {
      setErrorMessage("It was not possible to load the K3d configuration: " + (e as Error).message);
    }
    document.getElementById(formId)?.removeAttribute("disabled");
  };

  /* eslint-disable react-hooks/exhaustive-deps -- The function initializeForm() will never change, therefor it is not a dependency. */
  useEffect(() => {
    initializeForm();
  }, [adminConfigResponse]);
  /* eslint-enable react-hooks/exhaustive-deps */

  const reloadForm = async () => {
    await loadConfiguration();
    setCompleted(false);
  };

  const handleSubmit = async (values: typeof form.values) => {
    setErrorMessage("");
    const formData: FormData = new FormData();
    setKubeconfig(formData, values.kubeconfig);
    setCpuLimit(formData, values.cpuLimit);
    setMemoryLimit(formData, values.memoryLimit, values.memoryLimitUnit);
    try {
      if (!adminConfigResponse.kubeconfigUploaded) {
        await postAdminConfig(formData);
      } else {
        await putAdminConfig(formData);
      }
      setCompleted(true);
    } catch (e) {
      setErrorMessage(
        "It was not possible to submit the admin configuration: " + (e as Error).message
      );
    }
  };

  const handleDelete = async () => {
    setErrorMessage("");
    try {
      await deleteAdminConfig();
      setCompleted(true);
    } catch (e) {
      setErrorMessage(
        "It was not possible to delete the admin configuration: " + (e as Error).message
      );
    }
  };

  const setKubeconfig = (setInFormData: FormData, kubeconfig: File | null) => {
    if (kubeconfig !== null) {
      setInFormData.set(kubeconfigFormKey, kubeconfig);
    }
  };

  const setCpuLimit = (setInFormData: FormData, cpuLimit: number | string) => {
    if (cpuLimit !== "") {
      setInFormData.set(cpuLimitFormKey, String(cpuLimit));
    }
  };

  const setMemoryLimit = (
    setInFormData: FormData,
    memoryLimit: string,
    memoryLimitUnit: MemoryUnit
  ) => {
    if (memoryLimit !== "number") {
      setInFormData.set(
        memoryLimitFormKey,
        memorySpecificationToString({ value: Number.parseInt(memoryLimit), unit: memoryLimitUnit })
      );
    }
  };

  // confirmation, that form was submitted
  if (completed) {
    return (
      <Stack>
        <Text>The changes were saved.</Text>
        <Button id="admin-config-form-back-button" onClick={() => void reloadForm()}>
          Back
        </Button>
      </Stack>
    );
  }

  return (
    <form onSubmit={form.onSubmit(handleSubmit)} encType="multipart/form-data">
      <Text fw={600} size="lg">
        K3d Configuration
      </Text>
      <Text id={infoFieldId} c="red" size="sm" mt={4}>
        {errorMessage}
      </Text>
      <Text c="dimmed" size="sm" mt={4}>
        Required fields are marked with *
      </Text>

      <Fieldset id={formId} disabled>
        <Grid>
          <Grid.Col span={12}>
            <NumberInput
              id="cpu-limit-input"
              label="CPU limit"
              description="How much CPU one single pod can at maximum use."
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
              id="memory-limit-input"
              label="Memory limit"
              description="How much memory one single pod can at maximum use."
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
              id="memory-unit-input"
              label="Memory unit"
              description="Please select a unit for the desired memory limit."
              key={form.key("memoryLimitUnit")}
              {...form.getInputProps("memoryLimitUnit")}
              data={memoryUnits}
              allowDeselect={false}
              required={adminConfigResponse.memoryLimit !== null}
              withAsterisk={adminConfigResponse.memoryLimit !== null}
            />
          </Grid.Col>
          <Grid.Col span={12}>
            <FileInput
              id="kubeconfig-input"
              withAsterisk={!adminConfigResponse.kubeconfigUploaded}
              label="Kubeconfig"
              description="Please upload your Kubeconfig file for the K3d cluster that manages the challenge pods."
              key={form.key(kubeconfigFormKey)}
              disabled={form.submitting}
              {...form.getInputProps(kubeconfigFormKey)}
            />
          </Grid.Col>
        </Grid>

        <Group justify="flex-end" mt="md">
          <Button id="admin-config-form-submit-button" type="submit" loading={form.submitting}>
            {!adminConfigResponse.kubeconfigUploaded
              ? "Create K3d configuration"
              : "Update K3d configuration"}
          </Button>
          <Button
            id="admin-config-form-delete-button"
            type="button"
            onClick={() => void handleDelete()}
            loading={form.submitting}
          >
            Delete K3d configuration
          </Button>
        </Group>
      </Fieldset>
    </form>
  );
}
