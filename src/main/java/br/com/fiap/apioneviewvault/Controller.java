package br.com.fiap.apioneviewvault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/files")  // Adiciona um prefixo para as rotas
public class Controller {

    record FileResponse(UUID link) {}

    @Autowired
    FileRepository fileRepository;

    // Método POST para upload de arquivo
    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Criação de um UUID para o arquivo
            UUID fileId = UUID.randomUUID();

            // Define o caminho onde o arquivo será salvo
            Path targetLocation = Paths.get("src/main/resources/static/files/" + fileId.toString() + "-" + file.getOriginalFilename());

            // Salva o arquivo no diretório especificado
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Criação de um registro no banco de dados
            FileStorage fileStorage = new FileStorage();
            fileStorage.setId(fileId);
            fileStorage.setPath(file.getOriginalFilename());

            fileRepository.save(fileStorage);

            // Retorna a resposta com o ID do arquivo
            return ResponseEntity.ok(new FileResponse(fileId));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao fazer upload do arquivo: " + e.getMessage());
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id) throws MalformedURLException {
        FileStorage fileStorage = fileRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Arquivo não encontrado no bd")
        );

        Path filePath = Paths.get("src/main/resources/static/files/" + fileStorage.getPath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Arquivo não encontrado ou não pode ser lido");
        }

        fileRepository.delete(fileStorage);

        String contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileStorage.getPath() + "\"")
                .body(resource);
    }

    @GetMapping("/list")
    public List<FileStorage> listFiles() {
        return fileRepository.findAll();
    }
}