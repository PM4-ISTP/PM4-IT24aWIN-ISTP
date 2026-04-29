"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Button,
  Center,
  Fieldset,
  Grid,
  Group,
  NumberInput,
  Select,
  Stack,
  Text,
} from "@mantine/core";
import { Dropzone } from "@mantine/dropzone";
import { useForm } from "@mantine/form";
import { IconCloudUpload, IconFile, IconX } from "@tabler/icons-react";
import { useApiClient } from "@/src/shared/lib/api/client";
import type { components } from "@/src/shared/lib/api/schema";
import {
  memorySpecificationToString,
  MemoryUnit,
  memoryUnits,
  stringToMemorySpecification,
} from "@/src/shared/lib/memoryUnit";
import { notifications } from "@mantine/notifications";

type AdminConfigResponse = components["schemas"]["AdminConfigResponse"];

type Props = {
  initialConfig: AdminConfigResponse;
};

export default function AdminConfigForm({ initialConfig }: Props) {
  const router = useRouter();
  const apiClient = useApiClient();

  const config: AdminConfigResponse = initialConfig;

  const defaultMemoryUnit = MemoryUnit.MebiByte;

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
        podTtlSeconds: adminConfig.podTtlSeconds ?? 3600,
        kubeconfig: null as File | null,
      };
    } catch {
      return {
        cpuLimit: "",
        memoryLimit: "",
        memoryLimitUnit: defaultMemoryUnit,
        podTtlSeconds: 3600,
        kubeconfig: null as File | null,
      };
    }
  };

  const openRef = useRef<() => void>(null!);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

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
      const podTtlSeconds =
        values.podTtlSeconds != null && values.podTtlSeconds !== ""
          ? Number(values.podTtlSeconds)
          : undefined;

      if (!config.kubeconfigUploaded) {
        const { error } = await apiClient.POST("/api/admin/config", {
          body: { kubeconfig: kubeconfigBase64, cpuLimit, memoryLimit, podTtlSeconds },
        });
        if (error) throw new Error(JSON.stringify(error));
      } else {
        const { error } = await apiClient.PUT("/api/admin/config", {
          body: { kubeconfig: kubeconfigBase64, cpuLimit, memoryLimit, podTtlSeconds },
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
        Kubernetes Configuration
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
          <Grid.Col span={12}>
            <Grid align="flex-end">
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
                  description="Unit for the memory limit above."
                  key={form.key("memoryLimitUnit")}
                  {...form.getInputProps("memoryLimitUnit")}
                  data={memoryUnits}
                  allowDeselect={false}
                  required={config.memoryLimit != null}
                  withAsterisk={config.memoryLimit != null}
                />
              </Grid.Col>
            </Grid>
          </Grid.Col>
          <Grid.Col span={12}>
            <NumberInput
              id="pod-ttl-input"
              label="Pod TTL (seconds)"
              description="How long a challenge pod stays alive before it is automatically cleaned up."
              key={form.key("podTtlSeconds")}
              {...form.getInputProps("podTtlSeconds")}
              min={60}
              max={86400}
              step={60}
              allowNegative={false}
              allowDecimal={false}
              clampBehavior="strict"
            />
          </Grid.Col>
          <Grid.Col span={12}>
            <Stack gap={4}>
              <Text size="sm" fw={500}>
                Kubeconfig
                {!config.kubeconfigUploaded && (
                  <Text component="span" c="red" ml={2}>
                    *
                  </Text>
                )}
              </Text>
              <Text size="xs" c="dimmed">
                Upload your Kubeconfig file for the Kubernetes cluster that manages the challenge
                pods.
              </Text>
              <Dropzone
                id="kubeconfig-input"
                aria-label="Kubeconfig"
                openRef={openRef}
                onDrop={(files) => {
                  const file = files[0] ?? null;
                  form.setFieldValue("kubeconfig", file);
                  form.clearFieldError("kubeconfig");
                  setSelectedFile(file);
                }}
                maxFiles={1}
                disabled={form.submitting}
                style={{
                  border: "2px dashed",
                  borderColor: form.errors.kubeconfig
                    ? "var(--mantine-color-red-6)"
                    : "rgba(255,255,255,0.15)",
                  borderRadius: 10,
                  background: "rgba(255,255,255,0.03)",
                  cursor: "pointer",
                  transition: "border-color 150ms ease, background 150ms ease",
                }}
              >
                <Center py="md">
                  <Dropzone.Accept>
                    <IconCloudUpload size={40} color="#2563eb" />
                  </Dropzone.Accept>
                  <Dropzone.Reject>
                    <IconX size={40} color="var(--mantine-color-red-6)" />
                  </Dropzone.Reject>
                  <Dropzone.Idle>
                    {selectedFile ? (
                      <Stack align="center" gap={6}>
                        <IconFile size={40} color="#94a3b8" />
                        <Text size="sm" c="dimmed">
                          {selectedFile.name}
                        </Text>
                        <Text size="xs" c="dimmed">
                          Click to replace
                        </Text>
                      </Stack>
                    ) : (
                      <Stack align="center" gap={6}>
                        <IconCloudUpload size={40} color="#94a3b8" />
                        <Text size="sm" fw={500}>
                          Drag &amp; drop your kubeconfig here
                        </Text>
                        <Text size="xs" c="dimmed">
                          or
                        </Text>
                        <Button
                          size="xs"
                          radius="md"
                          aria-label="Kubeconfig"
                          style={{
                            background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                            border: "none",
                            fontWeight: 600,
                            boxShadow: "0 2px 8px rgba(79,70,229,0.3)",
                          }}
                          onClick={(e) => {
                            e.stopPropagation();
                            openRef.current?.();
                          }}
                        >
                          Browse file
                        </Button>
                      </Stack>
                    )}
                  </Dropzone.Idle>
                </Center>
              </Dropzone>
              {form.errors.kubeconfig && (
                <Text size="xs" c="red">
                  {form.errors.kubeconfig}
                </Text>
              )}
              {config.kubeconfigUploaded && !selectedFile && (
                <Text size="xs" c="dimmed">
                  A kubeconfig is already uploaded. Upload a new file only if you want to replace
                  it.
                </Text>
              )}
            </Stack>
          </Grid.Col>
        </Grid>

        <Group justify="space-between" mt="md">
          <Button
            id="admin-config-form-submit-button"
            type="submit"
            loading={form.submitting}
            radius="md"
            style={{
              background: "linear-gradient(90deg, #2563eb, #4f46e5)",
              border: "none",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 600,
              boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
            }}
          >
            {!config.kubeconfigUploaded
              ? "Create Kubernetes configuration"
              : "Update Kubernetes configuration"}
          </Button>
          <Button
            id="admin-config-form-delete-button"
            type="button"
            onClick={() => void handleDelete()}
            loading={form.submitting}
            radius="md"
            style={{
              background: "linear-gradient(90deg, #dc2626, #b91c1c)",
              border: "none",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 600,
              boxShadow: "0 2px 12px rgba(220,38,38,0.3)",
            }}
          >
            Delete Kubernetes configuration
          </Button>
        </Group>
      </Fieldset>
    </form>
  );
}
