package gg.blackdev;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import org.objectweb.asm.*;

public class SimpleObfuscator extends JFrame {

    private JTextArea outputArea;
    private JButton obfuscateButton;
    private JButton loadButton;
    private JButton saveButton;
    private File selectedJarFile;
    private File tempDir;

    public SimpleObfuscator() {
        setTitle("Java Obfuscator | by BlackDev");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


        outputArea = new JTextArea(15, 50);
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);


        JPanel buttonsPanel = new JPanel();
        obfuscateButton = new JButton("Obfuscate JAR");
        loadButton = new JButton("Load JAR File");
        saveButton = new JButton("Save Obfuscated JAR");
        saveButton.setEnabled(false);

        buttonsPanel.add(loadButton);
        buttonsPanel.add(obfuscateButton);
        buttonsPanel.add(saveButton);

        add(outputScrollPane, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);


        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadJarFile();
            }
        });

        obfuscateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedJarFile != null) {
                    try {
                        obfuscateJar(selectedJarFile);
                    } catch (Exception ex) {
                        outputArea.append("Error obfuscating JAR: " + ex.getMessage() + "\n");
                    }
                } else {
                    outputArea.append("No JAR file selected!\n");
                }
            }
        });

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveObfuscatedJar();
            }
        });
    }


    private void loadJarFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            selectedJarFile = fileChooser.getSelectedFile();
            outputArea.append("Loaded JAR: " + selectedJarFile.getName() + "\n");
            saveButton.setEnabled(false);
        }
    }


    private void obfuscateJar(File jarFile) throws IOException {
        tempDir = new File("obfuscated_temp");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }


        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                File entryFile = new File(tempDir, entry.getName());


                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                    continue;
                }


                try (InputStream is = jar.getInputStream(entry)) {
                    entryFile.getParentFile().mkdirs();
                    try (OutputStream os = new FileOutputStream(entryFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
            }
        }


        obfuscateClassesInDirectory(tempDir);

        outputArea.append("Obfuscation complete! Ready to save the obfuscated JAR.\n");
        saveButton.setEnabled(true);
    }


    private void obfuscateClassesInDirectory(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                obfuscateClassesInDirectory(file);
            } else if (file.getName().endsWith(".class")) {
                outputArea.append("Obfuscating: " + file.getName() + "\n");
                obfuscateClassFile(file);
            }
        }
    }


    private void obfuscateClassFile(File classFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(classFile)) {
            ClassReader classReader = new ClassReader(fis);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);


            classReader.accept(new ClassVisitor(Opcodes.ASM9, classWriter) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {

                    String obfuscatedName = "m_" + new Random().nextInt(1000);
                    return super.visitMethod(access, obfuscatedName, descriptor, signature, exceptions);
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {

                    String obfuscatedField = "f_" + new Random().nextInt(1000);
                    return super.visitField(access, obfuscatedField, descriptor, signature, value);
                }
            }, 0);


            try (FileOutputStream fos = new FileOutputStream(classFile)) {
                fos.write(classWriter.toByteArray());
            }
        }
    }

    //
    private void saveObfuscatedJar() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showSaveDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            File outputJarFile = fileChooser.getSelectedFile();

            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJarFile))) {
                addDirectoryToJar(tempDir, tempDir, jos);
            } catch (IOException e) {
                outputArea.append("Error saving obfuscated JAR: " + e.getMessage() + "\n");
            }

            outputArea.append("Saved obfuscated JAR: " + outputJarFile.getName() + "\n");
        }
    }


    private void addDirectoryToJar(File rootDir, File sourceDir, JarOutputStream jos) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String entryName = rootDir.toPath().relativize(file.toPath()).toString();
            if (file.isDirectory()) {
                jos.putNextEntry(new ZipEntry(entryName + "/"));
                jos.closeEntry();
                addDirectoryToJar(rootDir, file, jos);
            } else {
                jos.putNextEntry(new ZipEntry(entryName));
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        jos.write(buffer, 0, len);
                    }
                }
                jos.closeEntry();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SimpleObfuscator().setVisible(true);
            }
        });
    }
}
