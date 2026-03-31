"use client";

import { useRouter } from "next/navigation";
import {
  Button,
  Fieldset,
  FileInput,
  Grid,
  Group,
  NumberInput,
  Select,
  Text,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useApiClient } from "@/src/lib/api/client";
import type { components } from "@/src/lib/api/schema";
import {
  memorySpecificationToString,
  MemoryUnit,
  memoryUnits,
  stringToMemorySpecification,
} from "@/src/lib/memoryUnit";
import { notifications } from "@mantine/notifications";

type AdminConfigResponse = components["schemas"]["AdminConfigResponse"];

type Props = {
  initialConfig: AdminConfigResponse;
};

export default function AdminConfigForm({ initialConfig }: Props) {
  const router = useRouter();
  const apiClient = useApiClient();

  const config: AdminConfigResponse = initialConfig;

  const defaultMemoryUnit = MemoryUnit.Byte;

  const getInitialFormValues = (adminConfig: AdminConfigResponse) => {
    try {
      const memorySpecification =
        adminConfig.memoryLimit == null
          ? null
          : stringToMemorySpecification(adminConfig.memoryLimit);
      const hasMemorySpecification = memorySpecification !== null;
      return {
        cpuLimit: adminConfig.cpuLimit ?? "",
        memoryLimit: hasMemorySpecification ? String(memorySpecification.value) : "",
        memoryLimitUnit: hasMemorySpecification ? memorySpecification.unit : defaultMemoryUnit,
        kubeconfig: null as File | null,
      };
    } catch {
      return {
        cpuLimit: "",
        memoryLimit: "",
        memoryLimitUnit: defaultMemoryUnit,
        kubeconfig: null as File | null,
      };
    }
  };

  const form = useForm({
    mode: "uncontrolled",
    initialValues: getInitialFormValues(config),
    validate: {
      // In Firefox, the required attribute on FileInput does nothing. Therefor, custom validation is needed.
      kubeconfig: (value) => {
        if (!config.kubeconfigUploaded && value === null) {
          return "You need to upload a Kubeconfig file.";
        }
        return null;
      },
    },
  });

  const handleSubmit = async (values: typeof form.values) => {
    let kubeconfigBase64: string | undefined;
    if (values.kubeconfig !== null) {
      const buffer = await values.kubeconfig.arrayBuffer();
      kubeconfigBase64 = btoa(String.fromCharCode(...new Uint8Array(buffer)));
    }

    const cpuLimit = values.cpuLimit !== "" ? String(values.cpuLimit) : undefined;
    const memoryLimit =
      values.memoryLimit !== ""
        ? memorySpecificationToString({
            value: Number.parseInt(String(values.memoryLimit)),
            unit: values.memoryLimitUnit,
          })
        : undefined;

    try {
      if (!config.kubeconfigUploaded) {
        const { error } = await apiClient.POST("/api/admin/config", {
          body: { kubeconfig: kubeconfigBase64, cpuLimit, memoryLimit },
        });
        if (error) throw new Error(JSON.stringify(error));
      } else {
        const { error } = await apiClient.PUT("/api/admin/config", {
          body: { kubeconfig: kubeconfigBase64, cpuLimit, memoryLimit },
        });
        if (error) throw new Error(JSON.stringify(error));
      }
      notifications.show({
        title: "Success",
        message: "Admin configuration has been successfully updated.",
        color: "green",
      });
      router.refresh();
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "It was not possible to submit the admin configuration: " + (e as Error).message,
        color: "red",
      });
    }
  };

  const handleDelete = async () => {
    try {
      const { error } = await apiClient.DELETE("/api/admin/config");
      if (error) throw new Error(JSON.stringify(error));
      notifications.show({
        title: "Success",
        message: "Admin configuration has been successfully deleted.",
        color: "green",
      });
      router.refresh();
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "It was not possible to delete the admin configuration: " + (e as Error).message,
        color: "red",
      });
    }
  };

  return (
    <form onSubmit={form.onSubmit((values) => void handleSubmit(values))}>
      <Text fw={600} size="lg">
        K3d Configuration
      </Text>
      <Text c="dimmed" size="sm" mt={4}>
        Required fields are marked with *
      </Text>

      <Fieldset id="admin-config-form">
        <Grid>
          <Grid.Col span={12}>
            <NumberInput
              id="cpu-limit-input"
              label="CPU limit"
              description="How much CPU one single pod can at maximum use."
              key={form.key("cpuLimit")}
              {...form.getInputProps("cpuLimit")}
              min={1}
              allowNegative={false}
              allowDecimal={false}
              clampBehavior="strict"
              required={config.cpuLimit != null}
              withAsterisk={config.cpuLimit != null}
            />
          </Grid.Col>
          <Grid.Col span={8}>
            <NumberInput
              id="memory-limit-input"
              label="Memory limit"
              description="How much memory one single pod can at maximum use."
              key={form.key("memoryLimit")}
              {...form.getInputProps("memoryLimit")}
              min={1}
              allowNegative={false}
              allowDecimal={false}
              clampBehavior="strict"
              required={config.memoryLimit != null}
              withAsterisk={config.memoryLimit != null}
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
              required={config.memoryLimit != null}
              withAsterisk={config.memoryLimit != null}
            />
          </Grid.Col>
          <Grid.Col span={12}>
            <FileInput
              id="kubeconfig-input"
              withAsterisk={!config.kubeconfigUploaded}
              label="Kubeconfig"
              description="Please upload your Kubeconfig file for the K3d cluster that manages the challenge pods."
              key={form.key("kubeconfig")}
              disabled={form.submitting}
              {...form.getInputProps("kubeconfig")}
            />
          </Grid.Col>
        </Grid>

        <Group justify="flex-end" mt="md">
          <Button id="admin-config-form-submit-button" type="submit" loading={form.submitting}>
            {!config.kubeconfigUploaded ? "Create K3d configuration" : "Update K3d configuration"}
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
